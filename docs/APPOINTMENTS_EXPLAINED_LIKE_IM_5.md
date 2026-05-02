# Appointments Code Explained Like You're 5

This file is your "teacher-proof" guide.
It explains the appointments system in **very simple words**, even if you do not know Spring or Angular.

---

## 1) First: What Are Spring and Angular?

Imagine an app is a restaurant:

- **Angular (frontend)** = the waiter and menu you see.
  - Buttons, pages, forms.
  - Takes your click and sends requests.
- **Spring Boot (backend)** = the kitchen.
  - Real logic.
  - Checks rules.
  - Saves data in database.
- **Database** = fridge + notebook.
  - Stores appointments, teleconsultation links, statuses, etc.
- **Gateway** = restaurant entrance receptionist.
  - Every request enters from one door (`:8082`) and gets sent to the right kitchen.

So:
- Browser page asks for something.
- Angular sends request to gateway.
- Gateway sends to appointment-service (Spring).
- Spring does logic and replies.
- Angular shows result.

---

## 2) Big Picture of the Appointment Feature

Main things this feature does:

- Create appointment (patient asks, doctor schedules).
- List appointments.
- Open appointment details.
- Confirm, cancel, complete.
- Reschedule with conflict checks.
- Teleconsultation room (Jitsi).
- Live speech-to-text + translation for call captions.
- Notifications and reminder emails.

---

## 3) Where Code Lives (Important Files)

## Backend (Spring Boot)

- `services/appointment-service/src/main/java/com/example/appointment/controller/AppointmentController.java`
  - API endpoints (the "front desk" for appointment backend).
- `services/appointment-service/src/main/java/com/example/appointment/service/impl/AppointmentServiceImpl.java`
  - Main business logic (the brain).
- `services/appointment-service/src/main/java/com/example/appointment/entity/Appointment.java`
  - Appointment table shape.
- `services/appointment-service/src/main/java/com/example/appointment/entity/Teleconsultation.java`
  - Teleconsultation data shape.
- `services/appointment-service/src/main/java/com/example/appointment/repository/AppointmentRepository.java`
  - DB search methods.
- `services/appointment-service/src/main/java/com/example/appointment/repository/TeleconsultationRepository.java`
  - DB access for tele room data.
- `services/appointment-service/src/main/resources/application.yml`
  - Config: ports, URLs, keys, defaults.

## Frontend (Angular)

- `apps/web-app/src/app/core/services/appointment.service.ts`
  - Frontend HTTP methods for appointment APIs.
- `apps/web-app/src/app/features/appointments/appointment-list/appointment-list.component.ts`
  - Main list page.
- `apps/web-app/src/app/features/appointments/appointment-detail/appointment-detail.component.ts`
  - Detail page + teleconsultation + captions.
- `apps/web-app/src/app/features/appointments/appointment-book/appointment-book.component.ts`
  - Patient booking page.
- `apps/web-app/src/app/features/appointments/appointment-schedule/appointment-schedule.component.ts`
  - Doctor scheduling page.
- `apps/web-app/src/app/features/appointments/patient-appointments-hub/patient-appointments-hub.component.ts`
  - Patient dashboard view.
- `apps/web-app/src/app/features/appointments/appointment-calendar/appointment-calendar.component.ts`
  - Reusable month/day calendar UI.

## Gateway + Whisper Translation

- `services/API_Gatewya/src/main/resources/application.yml`
  - Routes `/api/appointments/**` and `/api/whisper/**`.
- `services/whisper-ai-service/app/main.py`
  - Speech transcription + translation backend.

---

## 4) Backend: AppointmentController (Simple Meaning of Each Endpoint)

Think: controller = "phone operator" that answers API calls and hands work to service.

- `getMyNotifications()`
  - Returns my appointment notifications.
- `getAppointmentNotificationsUnreadCount()`
  - Returns how many unread notification badges I have.
- `markAppointmentNotificationRead(notificationId)`
  - Marks one notification as read.
- `markAllAppointmentNotificationsRead()`
  - Marks all my appointment notifications as read.
- `patientRequest(request)`
  - Patient asks for an appointment.
- `doctorSchedule(request)`
  - Doctor directly schedules an appointment.
- `confirm(id)`
  - Doctor confirms a pending appointment.
- `cancel(id)`
  - Patient or doctor cancels appointment (with permission checks).
- `reschedule(id, request)`
  - Moves appointment to a new date/time.
- `complete(id)`
  - Doctor marks visit completed.
- `listAppointments(...)`
  - Returns appointments list (mine or all if admin).
- `calendarHints(...)`
  - Returns occupied times to prevent overlap.
- `getOne(id)`
  - Returns one appointment detail.
- `downloadCalendarIcs(id)`
  - Downloads calendar file (.ics) for calendar apps.
- `googleCalendarLink(id)`
  - Gives one-click Google Calendar URL for that visit.
- `getTele(id)`
  - Returns teleconsultation info (meeting URL/status).
- `startTele(id)`
  - Starts/joins teleconsultation session state.

---

## 5) Backend: AppointmentServiceImpl (The Brain)

Think: service = "rules teacher + decision maker."

- `patientRequest(...)`
  - Validates data.
  - Creates appointment as pending.
  - May create teleconsultation metadata if type is video.
  - Sends notifications.

- `doctorSchedule(...)`
  - Doctor schedules directly.
  - Checks rules and conflicts.
  - Creates appointment (and tele data if needed).

- `confirm(appointmentId, doctorUserId)`
  - Ensures doctor owns this appointment.
  - Changes status to confirmed.
  - Sends notifications/reminder logic.

- `cancel(appointmentId, currentUserId)`
  - Checks who is canceling and if allowed.
  - Sets status canceled.
  - Updates teleconsultation status if needed.

- `complete(appointmentId, doctorUserId)`
  - Only doctor can complete.
  - Sets appointment status to completed.
  - Closes teleconsultation status.

- `listMine(userId)`
  - Returns appointments where user is patient or doctor.

- `listAll()`
  - Returns all appointments (admin use).

- `getById(id, userId, admin)`
  - Gets one appointment.
  - Checks access rights.
  - Adds useful response data (names/tele info).

- `getTeleconsultation(appointmentId, userId, admin)`
  - Returns tele room metadata if user is allowed.

- `startTeleconsultation(appointmentId, userId)`
  - Verifies appointment is tele type.
  - Verifies status is valid.
  - Marks tele status `LIVE` and saves start time.

- `calendarHintsForPatient(...)`
  - Returns blocked slots relevant to patient.

- `calendarHintsForDoctor(...)`
  - Returns blocked slots relevant to doctor.

- `reschedule(appointmentId, currentUserId, request)`
  - Checks new date/time validity.
  - Prevents overlap.
  - Updates appointment timing.
  - Resets reminder dispatch markers.
  - Triggers reschedule notifications.

- `exportIcs(...)`
  - Builds iCalendar bytes for download.

- `googleCalendarLink(...)`
  - Builds Google Calendar link for that appointment.

---

## 6) Frontend: `appointment.service.ts` (Simple Meaning)

Think: this file is your "remote control" to backend.
Each method sends one HTTP request.

- `getGoogleCalendarStatus()`
  - Ask if Google Calendar account is linked.
- `getGoogleCalendarAuthorizeUrl(returnTo?)`
  - Ask backend for OAuth URL.
- `completeGoogleCalendarOAuth(code)`
  - Finish OAuth after Google sends code.
- `syncGoogleCalendar()`
  - Trigger sync of appointments to Google Calendar.
- `patientRequest(body)`
  - Send patient booking request.
- `doctorSchedule(body)`
  - Send doctor scheduling request.
- `confirm(id)`
  - Confirm appointment.
- `cancel(id)`
  - Cancel appointment.
- `reschedule(id, body)`
  - Reschedule appointment.
- `complete(id)`
  - Mark completed.
- `downloadCalendarIcs(id)`
  - Download .ics.
- `getGoogleCalendarLink(id)`
  - Get Google add-event URL.
- `getMine()`
  - Get my appointments.
- `getAll()`
  - Get all (admin).
- `getById(id)`
  - Get details.
- `getTeleconsultation(id)`
  - Get tele room info.
- `startTeleconsultation(id)`
  - Start/join tele status.
- `getAppointmentNotifications()`
  - Get notifications list.
- `getAppointmentNotificationsUnreadCount()`
  - Get unread count.
- `markAppointmentNotificationRead(id)`
  - Mark one read.
- `markAllAppointmentNotificationsRead()`
  - Mark all read.

---

## 7) Frontend Pages and What Their Functions Do

## A) `appointment-list.component.ts`

- `ngOnInit()`
  - Runs when page opens.
  - Loads data first time.
- `onGoogleCalendarSync()`
  - Sync button action.
- `ngOnDestroy()`
  - Cleanup subscriptions/timers.
- `setScope(all)`
  - Toggle between mine/all (usually admin behavior).
- `load(...)`
  - Main fetch function for this page.
- `onCalendarMonthChange(ev)`
  - User moved month in calendar UI.
- `onCalendarDaySelect(ymd)`
  - User picked date filter.
- `clearDateFilter()`
  - Remove date filter.
- `confirm(a, ev)`
  - Confirm one row.
- `cancel(a, ev)`
  - Cancel one row.
- `openRow(a)`
  - Navigate to details page.

## B) `appointment-book.component.ts` (patient creates request)

- `ngOnInit()`
  - Load doctors and initial state.
- `loadCalendarHints()`
  - Load busy times for chosen doctor/date range.
- `onDoctorChange()`
  - Recalculate when doctor changes.
- `onCalMonthChange(ev)`
  - Reload hints for another month.
- `onAppointmentDateChange(value)`
  - Date field changed; refresh conflict context.
- `submit()`
  - Validate and send booking request.
- `cancel()`
  - Leave page/back.

## C) `appointment-schedule.component.ts` (doctor schedules)

- `ngOnInit()`
  - Load patients and defaults.
- `loadCalendarHints()`
  - Load busy slots for selected patient/month.
- `onPatientChange()`
  - Re-run checks when patient changes.
- `onCalMonthChange(ev)`
  - Month changed; reload hints.
- `onAppointmentDateChange(value)`
  - Date changed; update checks.
- `submit()`
  - Validate and create scheduled appointment.
- `cancel()`
  - Leave/back.

## D) `patient-appointments-hub.component.ts`

- `ngOnInit()`
  - Load patient dashboard data.
- `onGoogleCalendarSync()`
  - Sync action.
- `ngOnDestroy()`
  - Cleanup.
- `load(...)`
  - Main data loading.
- `onCalMonthChange(ev)`
  - Calendar month changed.
- `onDateChange(value)`
  - Date filter changed.
- `openAppointment(a)`
  - Open detail page.
- `onAppointmentItemKeydown(event, a)`
  - Keyboard accessibility open behavior.
- `goBook()`
  - Go to booking page.
- `goList()`
  - Go to full list.

## E) `appointment-calendar.component.ts` (reusable calendar widget)

- `ngOnChanges(changes)`
  - Recompute view when inputs change.
- `prevMonth()`
  - Show previous month.
- `nextMonth()`
  - Show next month.
- `pickDay(cell)`
  - User picked day cell.

---

## 8) The Most Important File: `appointment-detail.component.ts`

This is the "everything page" for one appointment.

## Basic lifecycle

- `ngOnInit()`
  - Reads appointment ID from URL.
  - Loads appointment details.
  - Loads participant names.
  - If teleconsultation missing but needed, fetches it.

- `ngOnDestroy()`
  - Stops captions.
  - Closes meeting panel.

## Rescheduling part

- `openReschedulePanel()`
  - Shows reschedule UI and loads busy hints.
- `closeReschedulePanel()`
  - Hides reschedule UI.
- `loadRescheduleHints()`
  - Calls backend for occupied slots.
- `onRescheduleDateChange(value)`
  - Updates month/date and hints.
- `onRescheduleCalMonthChange(ev)`
  - Loads hints for selected month.
- `submitReschedule()`
  - Validates and sends reschedule request.

## Teleconsultation part

- `openInlineMeeting(url)`
  - Opens Jitsi inside page iframe.
- `closeInlineMeeting()`
  - Closes iframe and captions.
- `startVideo()`
  - Starts teleconsultation session via backend.
- `openRoom()`
  - Opens existing tele room.

## Captions + translation part (your recent changes)

- `toggleLiveCaptions()`
  - Start/stop live caption pipeline.
- `acquireCaptionInputStream()`
  - Gets audio source (tab audio preferred, mic fallback).
- `beginCaptionSegmentLoop()`
  - Records short audio chunks continuously.
- `uploadAudioChunk(blob, filename)`
  - Sends chunk to whisper transcription endpoint.
  - Skips chunks when local user is speaking (own-voice suppression).
- `scheduleAutoTranslate()`
  - Debounces translation requests to avoid too many API calls.
- `translateTranscript(isManual)`
  - Sends text to translation endpoint and updates translated captions.
- `stopLiveCaptions()`
  - Stops recorder, timers, streams, subscriptions.

## Misc detail actions

- `complete()`
  - Doctor marks visit completed.
- `back()`
  - Returns to list page.

---

## 9) End-to-End Story (One Full Example)

Patient books:

1. Patient fills form in `appointment-book.component.ts`.
2. Frontend calls `appointment.service.ts -> patientRequest(...)`.
3. Gateway routes `/api/appointments/...` to appointment-service.
4. `AppointmentController.patientRequest(...)` receives it.
5. `AppointmentServiceImpl.patientRequest(...)` validates, saves, notifies.
6. Response goes back to frontend and appears in list/detail.

During teleconsultation:

1. User opens detail page and starts session.
2. Jitsi room opens in iframe.
3. Caption system records audio chunks.
4. Chunks go to whisper service through gateway.
5. Whisper returns transcript.
6. Translation endpoint returns translated text.
7. UI shows one merged "translated live captions" area.

---

## 10) Quick Dictionary (Simple Words)

- **Controller**: door that receives API calls.
- **Service**: brain with business rules.
- **Repository**: helper that talks to database.
- **Entity**: object/table model in database.
- **DTO**: response/request data shape sent over API.
- **Component (Angular)**: one UI block/page with logic.
- **Observable**: async data stream (Angular/RxJS way to handle API replies).
- **JWT**: login token proving user identity.
- **OAuth**: "login/connect with Google" permission flow.
- **Teleconsultation**: online video appointment.

---

## 11) If Teacher Asks "Why split into so many files?"

Good answer:

- One file for API entry (controller).
- One file for business rules (service).
- One file for database operations (repository).
- One file for data shape (entity/DTO).
- One file for each UI page/component.

This keeps code easier to test, debug, and modify.

---

## 12) 30-Second Oral Summary You Can Memorize

"Appointments use Angular on the frontend and Spring Boot on the backend.  
Frontend components call methods in `appointment.service.ts`, which send requests to the gateway on port 8082.  
The gateway forwards appointment routes to appointment-service, where `AppointmentController` receives the request and `AppointmentServiceImpl` applies business rules like validation, overlap checks, status changes, teleconsultation setup, and notifications.  
Appointment detail also runs live captions by sending audio chunks to whisper service and then translates text for real-time translated captions."

