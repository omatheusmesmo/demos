/*
 * Discount rules, edited by the billing team without a redeploy of the Java code.
 * Amounts are JavaScript numbers (doubles); the caller rounds the result to cents.
 */
function discount(tier, subtotal) {
    if (tier === 'GOLD') {
        return subtotal * 0.10;
    }
    if (tier === 'SILVER' && subtotal > 500) {
        return subtotal * 0.05;
    }
    if (subtotal > 1000) {
        return 25;
    }
    return 0;
}
