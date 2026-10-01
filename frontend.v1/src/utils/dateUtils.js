// Accepts an ISO string ("2026-10-01T12:30:00") or Jackson's array form ([2026,10,1,12,30,0]).
export function parseDate(value) {
  if (!value) return null;
  if (Array.isArray(value)) {
    const [y, mo, d, h = 0, mi = 0, s = 0] = value;
    return new Date(y, mo - 1, d, h, mi, s);
  }
  const date = new Date(value);
  return isNaN(date.getTime()) ? null : date;
}
