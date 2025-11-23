import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule, FormControl } from '@angular/forms';
import { MultiSelectComponent } from './multi-select.component';

describe('MultiSelectComponent', () => {
  let component: MultiSelectComponent;
  let fixture: ComponentFixture<MultiSelectComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MultiSelectComponent, ReactiveFormsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(MultiSelectComponent);
    component = fixture.componentInstance;
    component.options = [
      { value: 'option1', label: 'Option 1' },
      { value: 'option2', label: 'Option 2' },
      { value: 'option3', label: 'Option 3' },
    ];
    component.control = new FormControl([]);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should toggle dropdown', () => {
    expect(component.isOpen()).toBe(false);
    component.toggleDropdown();
    expect(component.isOpen()).toBe(true);
    component.toggleDropdown();
    expect(component.isOpen()).toBe(false);
  });

  it('should select and deselect options', () => {
    const option = component.options[0];

    component.toggleOption(option);
    expect(component.selectedValues).toContain('option1');

    component.toggleOption(option);
    expect(component.selectedValues).not.toContain('option1');
  });

  it('should filter options based on search term', () => {
    component.searchTerm.set('option 1');
    const filtered = component.filteredOptions;

    expect(filtered.length).toBe(1);
    expect(filtered[0].value).toBe('option1');
  });

  it('should respect maxSelectedItems limit', () => {
    component.maxSelectedItems = 2;

    component.toggleOption(component.options[0]);
    component.toggleOption(component.options[1]);
    component.toggleOption(component.options[2]);

    expect(component.selectedValues.length).toBe(2);
  });

  it('should prevent removal of non-removable items', () => {
    component.itemsNotRemovable = ['option1'];
    component.selectedValues = ['option1', 'option2'];

    component.removeSelectedItem('option1', new Event('click'));
    expect(component.selectedValues).toContain('option1');

    component.removeSelectedItem('option2', new Event('click'));
    expect(component.selectedValues).not.toContain('option2');
  });
});
