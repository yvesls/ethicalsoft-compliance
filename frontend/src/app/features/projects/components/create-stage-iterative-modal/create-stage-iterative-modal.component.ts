import { Component, OnInit, Output, EventEmitter, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ModalService } from '../../../../core/services/modal.service';
import { InputComponent } from '../../../../shared/components/input/input.component';

export interface NewStageIterativeData {
  name: string;
  weight: number;
}

@Component({
  selector: 'app-create-stage-iterative-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, InputComponent],
  templateUrl: './create-stage-iterative-modal.component.html',
  styleUrls: ['./create-stage-iterative-modal.component.scss']
})
export class CreateStageIterativeModalComponent implements OnInit {
  @Output() stageCreated = new EventEmitter<NewStageIterativeData>();

  private modalService = inject(ModalService);
  private fb = inject(FormBuilder);

  form!: FormGroup;

  ngOnInit(): void {
    this.form = this.fb.group({
      name: ['', [Validators.required]],
      weight: [1, [Validators.required, Validators.min(1)]]
    });
  }

  confirm(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const newStage: NewStageIterativeData = {
      name: this.form.value.name,
      weight: this.form.value.weight
    };

    this.stageCreated.emit(newStage);

    this.modalService.close();
  }
}
