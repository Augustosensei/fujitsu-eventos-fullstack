import { Component, computed, input, output } from '@angular/core';

@Component({
  selector: 'app-paginator',
  styleUrl: './paginator.css',
  templateUrl: './paginator.html',
})
export class Paginator {
  /** Zero-based, as in the API. */
  readonly page = input.required<number>();
  readonly totalPages = input.required<number>();
  readonly pageChange = output<number>();

  protected readonly pageCount = computed(() => Math.max(this.totalPages(), 1));
  protected readonly isFirst = computed(() => this.page() <= 0);
  protected readonly isLast = computed(() => this.page() >= this.pageCount() - 1);
}
