"""Summarises one variant measured by run.sh as Markdown table rows, one row per arrival rate.

Usage: python3 -I summarize.py rows <label> <variant-dir>
       python3 -I summarize.py gc <label> <variant-dir>

Every value is the median across runs, with the range in parentheses when runs disagree, because averages hide
outliers. Errors count failed checks (non-200 answers and timeouts); dropped counts requests k6 could not even start
because every virtual user was busy, which means the server could not keep up with the arrival rate.
"""
import json
import pathlib
import re
import statistics
import sys

UNITS = {"B": 1 / 2**20, "KiB": 1 / 1024, "MiB": 1, "GiB": 1024}


def spread(values, digits=0):
    values = sorted(values)
    median = statistics.median(values)
    text = f"{median:.{digits}f}"
    if len(values) > 1 and f"{values[0]:.{digits}f}" != f"{values[-1]:.{digits}f}":
        text += f" ({values[0]:.{digits}f} to {values[-1]:.{digits}f})"
    return text


def run_metrics(summary_file):
    metrics = json.loads(summary_file.read_text())["metrics"]
    checks = metrics.get("checks", {})
    total = checks.get("passes", 0) + checks.get("fails", 0)
    duration = metrics.get("http_req_duration", {})
    return {
        "achieved": metrics.get("http_reqs", {}).get("rate", 0),
        "p50": duration.get("med", 0),
        "p99": duration.get("p(99)", 0),
        "errors": 100 * checks.get("fails", 0) / total if total else 0,
        "dropped": metrics.get("dropped_iterations", {}).get("count", 0),
    }


def resource_metrics(stats_file):
    cpu, memory = [], []
    for line in stats_file.read_text().splitlines():
        sample = json.loads(line)
        cpu.append(float(sample["CPUPerc"].rstrip("%")))
        used = re.match(r"([\d.]+)(\w+)", sample["MemUsage"])
        memory.append(float(used.group(1)) * UNITS[used.group(2)])
    return {"cpu": statistics.mean(cpu) if cpu else 0, "memory": max(memory) if memory else 0}


def gc_pauses(gc_log):
    pauses = [float(m.group(1)) for m in re.finditer(r"Pause.*?([\d.]+)ms\s*$", gc_log.read_text(), re.M)]
    collector = re.search(r"Using (\w+)", gc_log.read_text())
    return collector.group(1) if collector else "unknown", pauses


def rows(label, variant_dir):
    rate_dirs = sorted(variant_dir.glob("rate-*"), key=lambda d: int(d.name.split("-")[1]))
    for rate_dir in rate_dirs:
        runs = sorted(rate_dir.glob("run-*.json"))
        k6 = [run_metrics(run) for run in runs]
        resources = [resource_metrics(run.with_suffix(".stats.jsonl")) for run in runs]
        loads = [float(f.read_text()) for f in rate_dir.glob("run-*.load-*")]
        print(f"| {label} | {rate_dir.name.split('-')[1]} "
              f"| {spread([m['achieved'] for m in k6])} "
              f"| {spread([m['p50'] for m in k6])} "
              f"| {spread([m['p99'] for m in k6])} "
              f"| {spread([m['errors'] for m in k6], 1)} "
              f"| {spread([m['dropped'] for m in k6])} "
              f"| {spread([r['cpu'] for r in resources])} "
              f"| {spread([r['memory'] for r in resources])} "
              f"| {max(loads):.1f} |")


def gc(label, variant_dir):
    gc_log = variant_dir / "gc" / "gc.log"
    if not gc_log.exists():
        return
    collector, pauses = gc_pauses(gc_log)
    longest = max(pauses) if pauses else 0
    print(f"- {label}: {collector}, {len(pauses)} pauses, {sum(pauses):.0f} ms in total, longest {longest:.1f} ms")


if __name__ == "__main__":
    {"rows": rows, "gc": gc}[sys.argv[1]](sys.argv[2], pathlib.Path(sys.argv[3]))
