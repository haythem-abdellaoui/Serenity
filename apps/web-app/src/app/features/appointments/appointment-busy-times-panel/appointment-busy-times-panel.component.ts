import { Component, Input } from '@angular/core';
import { CalendarBusySlot } from '../../../shared/models/appointment.model';
import {
  APPOINTMENT_SLOT_DURATION_MINUTES,
  formatSlotRange,
  slotsForDate
} from '../../../shared/utils/appointment-scheduling.utils';

@Component({
  selector: 'app-appointment-busy-times-panel',
  templateUrl: './appointment-busy-times-panel.component.html',
  styleUrls: ['./appointment-busy-times-panel.component.scss']
})
export class AppointmentBusyTimesPanelComponent {
  @Input() busySlots: CalendarBusySlot[] = [];
  @Input() selectedDate = '';
  @Input() slotDurationMinutes = APPOINTMENT_SLOT_DURATION_MINUTES;
  @Input() legendDoctor = 'Doctor';
  @Input() legendPatient = 'Patient / you';
  @Input() heading = 'Taken times this day';
  @Input() subheading = 'Avoid picking the same start time (HH:mm).';

  get rows(): CalendarBusySlot[] {
    return slotsForDate(this.busySlots, this.selectedDate);
  }

  rangeLabel(s: CalendarBusySlot): string {
    return formatSlotRange(s.timeSlot, this.slotDurationMinutes);
  }

  sourceLabel(s: CalendarBusySlot): string {
    return s.source === 'DOCTOR' ? this.legendDoctor : this.legendPatient;
  }
}
