import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { EVENT_TYPE_LABELS } from '../../models/event-type';
import { TABLE_PAGE_SIZE } from '../../models/page';
import { EventApi } from '../../services/event-api';
import { toUiError } from '../../services/ui-error-mapper';
import { Paginator } from '../shared/paginator/paginator';
import { StatusMessage } from '../shared/status-message/status-message';

@Component({
  selector: 'app-event-list',
  imports: [DatePipe, RouterLink, Paginator, StatusMessage],
  styleUrl: './event-list.css',
  templateUrl: './event-list.html',
})
export class EventList {
  protected readonly typeLabels = EVENT_TYPE_LABELS;
  protected readonly page = signal(0);
  protected readonly events = inject(EventApi).page(
    computed(() => ({ page: this.page(), size: TABLE_PAGE_SIZE })),
  );
  protected readonly error = computed(() => {
    const error = this.events.error();
    return error ? toUiError(error) : null;
  });
}
