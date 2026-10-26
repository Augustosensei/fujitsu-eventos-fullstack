import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Paginator } from './paginator';

describe('Paginator', () => {
  let fixture: ComponentFixture<Paginator>;
  let emitted: number[];

  beforeEach(() => {
    fixture = TestBed.createComponent(Paginator);
    emitted = [];
    fixture.componentInstance.pageChange.subscribe((page) => emitted.push(page));
  });

  async function render(page: number, totalPages: number): Promise<HTMLButtonElement[]> {
    fixture.componentRef.setInput('page', page);
    fixture.componentRef.setInput('totalPages', totalPages);
    await fixture.whenStable();
    return [...(fixture.nativeElement as HTMLElement).querySelectorAll('button')];
  }

  it('UI-09 given a middle page when the buttons are clicked then it emits the previous and the next page', async () => {
    const [previous, next] = await render(1, 3);

    expect(previous.disabled).toBe(false);
    expect(next.disabled).toBe(false);
    previous.click();
    next.click();

    expect(emitted).toEqual([0, 2]);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Página 2 de 3');
  });

  it('UI-09 given a single page when it renders then both edge buttons are disabled', async () => {
    const [previous, next] = await render(0, 1);

    expect(previous.disabled).toBe(true);
    expect(next.disabled).toBe(true);
  });

  it('UI-09 given zero pages when it renders then it still reads page 1 of 1', async () => {
    await render(0, 0);

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Página 1 de 1');
  });
});
