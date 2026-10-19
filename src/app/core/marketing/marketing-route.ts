import { ActivatedRouteSnapshot } from '@angular/router';
export function marketingPath(url:string):string {
  const path=(url||'/').split(/[?#]/)[0];return path.length>1?path.replace(/\/+$/,''):path||'/';
}
export function hasMarketingTabs(root:ActivatedRouteSnapshot,url:string):boolean {
  let active=root;while(active.firstChild)active=active.firstChild;
  return marketingPath(url)==='/' || !!active.data['marketingLanding'];
}
