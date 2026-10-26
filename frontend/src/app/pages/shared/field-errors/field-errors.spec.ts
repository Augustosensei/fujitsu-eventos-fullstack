import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { form, validate } from '@angular/forms/signals';
import { FieldErrors, INVALID_VALUE_MESSAGE } from './field-errors';

describe('FieldErrors', () => {
  function render(errorMessage?: string) {
    const field = TestBed.runInInjectionContext(() =>
      form(signal(''), (path) => validate(path, () => ({ kind: 'parse', message: errorMessage }))),
    );
    const fixture = TestBed.createComponent(FieldErrors);
    fixture.componentRef.setInput('state', field());
    return { field, fixture };
  }

  function messages(element: HTMLElement): string[] {
    return [...element.querySelectorAll('.field-error')].map((p) => p.textContent!.trim());
  }

  it('UI-01 given an invalid field when it has not been touched then no message is shown', async () => {
    const { fixture } = render('Mensaje');
    await fixture.whenStable();

    expect(messages(fixture.nativeElement)).toEqual([]);
  });

  it('UI-01 given a touched invalid field when it renders then its message is shown', async () => {
    const { field, fixture } = render('Mensaje');
    field().markAsTouched();
    await fixture.whenStable();

    expect(messages(fixture.nativeElement)).toEqual(['Mensaje']);
  });

  it('UI-01 given an error without message (incomplete native date) when it renders then a generic hint is shown', async () => {
    const { field, fixture } = render();
    field().markAsTouched();
    await fixture.whenStable();

    expect(messages(fixture.nativeElement)).toEqual([INVALID_VALUE_MESSAGE]);
  });
});
