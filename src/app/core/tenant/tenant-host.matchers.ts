import { inject } from '@angular/core';
import { CanMatchFn } from '@angular/router';
import { PlatformHostService } from '../platform/platform-host.service';

export const tenantHostMatch: CanMatchFn = () => {
  const platformHost = inject(PlatformHostService);
  return platformHost.isCustomTenantHost();
};
