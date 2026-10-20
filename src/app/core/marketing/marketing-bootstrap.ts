import { DOCUMENT, isPlatformBrowser } from '@angular/common';
import { PLATFORM_ID, inject } from '@angular/core';
import { PlatformHostService } from '../platform/platform-host.service';

/** Starts critical marketing assets before initial navigation, using the existing host authority. */
export function prepareMarketingHome(loadHome:()=>Promise<unknown>=()=>import('../../pages/fluora-home/fluora-home.component')):void {
  if(!isPlatformBrowser(inject(PLATFORM_ID)))return;
  const host=inject(PlatformHostService),document=inject(DOCUMENT);
  if(host.isCustomTenantHost()||host.isAppHost()||document.location?.pathname!=='/')return;
  for(const file of ['manrope','plusjakartasans']){
    const href=`/fonts/marketing/${file}.woff2`;
    if(document.head.querySelector(`link[rel="preload"][href="${href}"]`))continue;
    const link=document.createElement('link');link.rel='preload';link.href=href;link.as='font';link.type='font/woff2';link.crossOrigin='anonymous';document.head.appendChild(link);
  }
  // A failed warm-up must not interfere with the router's normal loading/error handling.
  void loadHome().catch(()=>{});
}
