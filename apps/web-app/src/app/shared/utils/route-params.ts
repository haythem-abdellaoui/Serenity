import { ActivatedRoute } from '@angular/router';

export function getParamFromRouteTree(route: ActivatedRoute, param: string): string | null {
  let r: ActivatedRoute | null = route;
  while (r) {
    const v = r.snapshot.paramMap.get(param);
    if (v !== null) return v;
    r = r.parent;
  }
  return null;
}
