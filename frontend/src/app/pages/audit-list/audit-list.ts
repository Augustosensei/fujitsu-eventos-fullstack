import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TABLE_PAGE_SIZE } from '../../models/page';
import { AuditApi } from '../../services/audit-api';
import { toUiError } from '../../services/ui-error-mapper';
import { Paginator } from '../shared/paginator/paginator';
import { StatusMessage } from '../shared/status-message/status-message';

@Component({
  selector: 'app-audit-list',
  imports: [DatePipe, RouterLink, Paginator, StatusMessage],
  styleUrl: './audit-list.css',
  templateUrl: './audit-list.html',
})
export class AuditList {
  protected readonly page = signal(0);
  protected readonly audits = inject(AuditApi).page(
    computed(() => ({ page: this.page(), size: TABLE_PAGE_SIZE })),
  );
  protected readonly error = computed(() => {
    const error = this.audits.error();
    return error ? toUiError(error) : null;
  });
}
