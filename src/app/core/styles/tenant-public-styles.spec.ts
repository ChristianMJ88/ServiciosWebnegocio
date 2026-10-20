import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { signal } from '@angular/core';
import { vi } from 'vitest';
import { TENANT_PUBLIC_STYLES, createTenantPublicStyles, tenantPublicStylesResolver } from './tenant-public-styles';

describe('public tenant stylesheet lifecycle', () => {
  it('does nothing by default in application/tests', async () => {
    const styles = TestBed.inject(TENANT_PUBLIC_STYLES);
    expect(styles.ready()).toBe(true);
    await styles.ensure();
    expect(document.querySelector('[data-tenant-public-styles]')).toBeNull();
  });
  it('coalesces callers, preserves the cascade, and waits for load', async () => {
    const doc = document.implementation.createHTMLDocument();
    const base = doc.createElement('base'); base.href = 'https://example.test/'; doc.head.append(base);
    const global = doc.createElement('link'); global.rel = 'stylesheet'; global.href = 'styles.hash.css'; doc.head.append(global);
    const component = doc.createElement('style'); component.textContent = 'h1{color:red}'; doc.head.append(component);
    const styles = createTenantPublicStyles(doc);
    const first = styles.ensure(), second = styles.ensure();
    expect(first).toBe(second);
    const link = doc.head.querySelector<HTMLLinkElement>('[data-tenant-public-styles]')!;
    expect(link.href).toBe('https://example.test/tenant-public.css');
    expect(global.nextSibling).toBe(link); expect(link.nextSibling).toBe(component);
    expect(styles.ready()).toBe(false);
    link.dispatchEvent(new Event('load')); await first;
    expect(styles.ready()).toBe(true);
    await styles.ensure(); expect(doc.head.querySelectorAll('[data-tenant-public-styles]')).toHaveLength(1);
  });
  it('does not hang navigation on failure and allows retry', async () => {
    const doc = document.implementation.createHTMLDocument();
    const base = doc.createElement('base'); base.href = 'https://example.test/'; doc.head.append(base);
    const global = doc.createElement('link'); global.rel = 'stylesheet'; global.href = 'styles.css'; doc.head.append(global);
    const error = vi.spyOn(console, 'error').mockImplementation(() => {});
    const styles = createTenantPublicStyles(doc);
    const first = styles.ensure();
    doc.head.querySelector('[data-tenant-public-styles]')!.dispatchEvent(new Event('error')); await first;
    expect(styles.ready()).toBe(true); expect(doc.head.querySelector('[data-tenant-public-styles]')).toBeNull();
    const retry = styles.ensure(); expect(styles.ready()).toBe(true);
    doc.head.querySelector('[data-tenant-public-styles]')!.dispatchEvent(new Event('load')); await retry;
    expect(styles.ready()).toBe(true); expect(error).toHaveBeenCalledOnce(); error.mockRestore();
  });
  it('releases a stalled navigation without duplicating a late CSS request', async () => {
    vi.useFakeTimers();
    const error = vi.spyOn(console, 'error').mockImplementation(() => {});
    try {
      const doc = document.implementation.createHTMLDocument();
      const base = doc.createElement('base'); base.href = 'https://example.test/'; doc.head.append(base);
      const global = doc.createElement('link'); global.rel = 'stylesheet'; global.href = 'styles.css'; doc.head.append(global);
      const styles = createTenantPublicStyles(doc, 1000);
      const first = styles.ensure();
      const link = doc.head.querySelector('[data-tenant-public-styles]')!;
      await vi.advanceTimersByTimeAsync(999); expect(styles.ready()).toBe(false);
      await vi.advanceTimersByTimeAsync(1); await first;
      expect(styles.ready()).toBe(true); expect(styles.ensure()).toBe(first);
      expect(doc.head.querySelectorAll('[data-tenant-public-styles]')).toHaveLength(1);
      link.dispatchEvent(new Event('load')); await styles.ensure();
      expect(doc.head.querySelector('[data-tenant-public-styles]')).toBe(link);
    } finally { vi.useRealTimers(); error.mockRestore(); }
  });
  it('keeps critical CSS before tenant CSS when the expected global link is missing', async () => {
    const error = vi.spyOn(console, 'error').mockImplementation(() => {});
    const doc = document.implementation.createHTMLDocument();
    const base = doc.createElement('base'); base.href = 'https://example.test/'; doc.head.append(base);
    const critical = doc.createElement('style'); critical.textContent = 'body{margin:0}'; doc.head.append(critical);
    const component = doc.createElement('style'); component.textContent = 'h1[_ngcontent-x]{color:red}'; doc.head.append(component);
    const styles = createTenantPublicStyles(doc); const waiting = styles.ensure();
    const link = doc.head.querySelector('[data-tenant-public-styles]')!;
    expect(critical.nextSibling).toBe(link); expect(link.nextSibling).toBe(component);
    link.dispatchEvent(new Event('load')); await waiting; expect(error).toHaveBeenCalledOnce(); error.mockRestore();
  });
  it('appends after the global stylesheet when it is the last head child', async () => {
    const doc = document.implementation.createHTMLDocument();
    const base = doc.createElement('base'); base.href = 'https://example.test/'; doc.head.append(base);
    const component = doc.createElement('style'); component.textContent = 'h1[_ngcontent-x]{color:red}'; doc.head.append(component);
    const global = doc.createElement('link'); global.rel = 'stylesheet'; global.href = 'styles.css'; doc.head.append(global);
    const styles = createTenantPublicStyles(doc); const waiting = styles.ensure();
    const link = doc.head.querySelector('[data-tenant-public-styles]')!;
    expect(global.nextSibling).toBe(link); expect(doc.head.lastChild).toBe(link);
    link.dispatchEvent(new Event('load')); await waiting;
  });
  it('keeps the resolver pending until CSS is available', async () => {
    let complete!: () => void;
    const pending = new Promise<void>(resolve => complete = resolve);
    TestBed.configureTestingModule({ providers: [{ provide: TENANT_PUBLIC_STYLES, useValue: { ready: signal(false), ensure: () => pending } }] });
    const result = TestBed.runInInjectionContext(() => tenantPublicStylesResolver({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot)) as Promise<true>;
    let resolved = false; void result.then(() => resolved = true);
    await Promise.resolve(); expect(resolved).toBe(false);
    complete(); expect(await result).toBe(true);
  });
});
