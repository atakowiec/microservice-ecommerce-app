export function getSafeReturnUrl(value: string | null, fallback: string): string {
  return value?.startsWith('/') && !value.startsWith('//') ? value : fallback;
}
