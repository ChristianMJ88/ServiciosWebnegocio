import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { PlatformHostService } from '../../core/platform/platform-host.service';

type PlatformDestination = 'app' | 'marketing';

@Component({
  selector: 'app-platform-redirect',
  standalone: true,
  template: '<p>Redirigiendo a Fluora…</p>'
})
export class PlatformRedirectComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly platformHost = inject(PlatformHostService);

  ngOnInit(): void {
    const destination = this.route.snapshot.data['destination'] as PlatformDestination;
    const configuredPath = this.route.snapshot.data['path'] as string | undefined;
    const currentPath = globalThis.location?.pathname || '/';
    const path = configuredPath || currentPath;
    const suffix = `${globalThis.location?.search || ''}${globalThis.location?.hash || ''}`;
    const url = destination === 'marketing'
      ? this.platformHost.marketingUrl(path)
      : this.platformHost.appUrl(path);

    globalThis.location.replace(`${url}${suffix}`);
  }
}
