import { TestBed } from '@angular/core/testing';
import { StatusKind, StatusMessage } from './status-message';

describe('StatusMessage', () => {
  it.each<[StatusKind, string, string]>([
    ['loading', 'status', 'polite'],
    ['info', 'status', 'polite'],
    ['success', 'status', 'polite'],
    ['error', 'alert', 'assertive'],
  ])(
    'UI-05 given the %s kind when it renders then it is a %s live region (%s) with an icon',
    async (kind, role, live) => {
      const fixture = TestBed.createComponent(StatusMessage);
      fixture.componentRef.setInput('kind', kind);
      await fixture.whenStable();

      const host = fixture.nativeElement as HTMLElement;
      expect(host.getAttribute('role')).toBe(role);
      expect(host.getAttribute('aria-live')).toBe(live);
      expect(host.classList).toContain(`message--${kind}`);
      expect(host.querySelector('svg[aria-hidden="true"]')).not.toBeNull();
    },
  );

  it('UI-05 given an error message when it renders then the icon accompanies the text (color is never the only cue)', async () => {
    const fixture = TestBed.createComponent(StatusMessage);
    fixture.componentRef.setInput('kind', 'error');
    await fixture.whenStable();

    const icon = (fixture.nativeElement as HTMLElement).querySelector('svg')!;
    expect(icon.querySelectorAll('path').length).toBe(2);
  });
});
