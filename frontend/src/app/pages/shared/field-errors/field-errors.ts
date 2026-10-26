import { Component, computed, input } from '@angular/core';
import { ReadonlyFieldState } from '@angular/forms/signals';

export const INVALID_VALUE_MESSAGE = 'El valor está incompleto o no es válido.';

@Component({
  selector: 'app-field-errors',
  styleUrl: './field-errors.css',
  templateUrl: './field-errors.html',
})
export class FieldErrors {
  readonly state = input.required<ReadonlyFieldState<unknown>>();

  protected readonly messages = computed(() => {
    const state = this.state();
    return state.touched()
      ? state.errors().map((error) => error.message ?? INVALID_VALUE_MESSAGE)
      : [];
  });
}
