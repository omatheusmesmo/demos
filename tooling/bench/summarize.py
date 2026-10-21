"""Summarises one variant measured by run.sh as Markdown table rows, one row per arrival rate.

Usage: python3 -I summarize.py rows <label> <variant-dir>

Every value is the median across iterations (one fresh JVM each), with the range in parentheses when they disagree,
because averages hide outliers.

- Achieved is successful (2xx) responses per second of the whole phase, from its start until the last response
  (requests still in flight when the arrival window closes are waited for), like the Quarkus reference lab computes
  throughput.
- Errors are requests that started but did not end in a 2xx: timeouts, connection errors and error statuses.
- Unsent are scheduled requests that never started inside the window because every load generator session or
  connection was busy with earlier ones: the server could not keep up with the arrival rate.
- CPU is the application container (200% is two CPUs), RSS the peak resident set of the JVM process.
- GC is the total of the collector pauses that fell inside the measured window.
- Other CPU is the busy time of the application CPUs that the container did not use: other processes of the host
  running on them. Near 0 means the measurement was not disturbed.
CPU, RSS and other CPU only use the samples taken inside the measured window.
"""
import datetime
import json
import os
import pathlib
import re
import statistics
import sys

GC_PAUSE = re.compile(r"^\[(\S+)\].*Pause.*?([\d.]+)ms\s*$", re.M)
CLOCK_TICKS = os.sysconf("SC_CLK_TCK")


def spread(values, digits=0):
    values = sorted(values)
    median = statistics.median(values)
    text = f"{median:.{digits}f}"
    if len(values) > 1 and f"{values[0]:.{digits}f}" != f"{values[-1]:.{digits}f}":
        text += f" ({values[0]:.{digits}f} to {values[-1]:.{digits}f})"
    return text


def seconds(duration):
    value = re.fullmatch(r"(\d+)(ms|s|m)", duration)
    return int(value.group(1)) * {"ms": 0.001, "s": 1, "m": 60}[value.group(2)]


def load_metrics(all_json):
    result = json.loads(all_json.read_text())
    params = result["info"]["params"]
    summary = next(s for s in result["stats"] if s["phase"] == "load")["total"]["summary"]
    window = seconds(params["DURATION"])
    ok = summary["extensions"]["http"]["status_2xx"]
    started = summary["requestCount"]
    percentiles = summary["percentileResponseTime"]
    return {
        "achieved": ok / ((summary["endTime"] - summary["startTime"]) / 1000),
        "p50": percentiles["50.0"] / 1e6,
        "p99": percentiles["99.0"] / 1e6,
        "errors": 100 * (started - ok) / started if started else 0,
        "unsent": max(0, round(int(params["RATE"]) * window) - started),
        "window": (summary["startTime"] / 1000, summary["endTime"] / 1000),
    }


def resources(samples_file, window):
    start, end = window
    lines = [line.split() for line in samples_file.read_text().splitlines()]
    inside = [line for line in lines if len(line) >= 4 and start <= int(line[0]) / 1000 <= end]
    if len(inside) < 2:
        return {"cpu": 0, "other": 0, "rss": 0}
    first, last = inside[0], inside[-1]
    elapsed = (int(last[0]) - int(first[0])) / 1000
    container = 100 * (int(last[1]) - int(first[1])) / 1e6 / elapsed
    cores = 100 * (int(last[2]) - int(first[2])) / CLOCK_TICKS / elapsed
    return {
        "cpu": container,
        "other": max(0.0, cores - container),
        "rss": max((int(line[4]) / 1024 for line in inside if len(line) > 4), default=0),
    }


def gc_pause_ms(gc_log, window):
    start, end = window
    total = 0.0
    for match in GC_PAUSE.finditer(gc_log.read_text() if gc_log.exists() else ""):
        moment = datetime.datetime.fromisoformat(match.group(1)).timestamp()
        if start <= moment <= end:
            total += float(match.group(2))
    return total


def rows(label, variant_dir):
    iterations = sorted(d for d in variant_dir.glob("iteration-*") if (d / "done").exists())
    rates = sorted({int(d.name.split("-")[1]) for i in iterations for d in i.glob("rate-*")})
    for rate in rates:
        loads, usage, pauses = [], [], []
        for iteration in iterations:
            rate_dir = iteration / f"rate-{rate}"
            metrics = load_metrics(rate_dir / "all.json")
            loads.append(metrics)
            usage.append(resources(rate_dir / "samples.txt", metrics["window"]))
            pauses.append(gc_pause_ms(iteration / "gc.log", metrics["window"]))
        print(f"| {label} | {rate} "
              f"| {spread([m['achieved'] for m in loads])} "
              f"| {spread([m['p50'] for m in loads])} "
              f"| {spread([m['p99'] for m in loads])} "
              f"| {spread([m['errors'] for m in loads], 1)} "
              f"| {spread([m['unsent'] for m in loads])} "
              f"| {spread([u['cpu'] for u in usage])} "
              f"| {spread([u['rss'] for u in usage])} "
              f"| {spread(pauses)} "
              f"| {spread([u['other'] for u in usage])} |")


if __name__ == "__main__":
    {"rows": rows}[sys.argv[1]](sys.argv[2], pathlib.Path(sys.argv[3]))
