export type Role = 'ADMIN' | 'VET' | 'RECEPTIONIST';
export type VisitStatus = 'SCHEDULED' | 'CHECKED_IN' | 'IN_EXAM' | 'COMPLETED' | 'CANCELLED';
export type InvoiceStatus = 'DRAFT' | 'SENT' | 'PAID';
export type InvoiceCategory = 'CONSULTATION' | 'VACCINATION' | 'SURGERY' | 'HOSPITAL' | 'MEDICATION' | 'OTHER';
export interface Base { id: number; createdAt: string; updatedAt: string }
export interface Account { id: number; email: string; fullName: string; role: Role; vetId: number | null }
export interface UserAccount extends Base, Account { enabled: boolean; vetName: string | null }
export interface Owner extends Base {firstName:string;lastName:string;phone:string|null;email:string|null;address:string|null;archived:boolean}
export interface Pet extends Base {ownerId:number;ownerName:string;name:string;species:string;breed:string|null;speciesNote:string|null;birthDate:string|null;sex:string|null;weightKg:number|null;allergies:string|null;chronicConditions:string|null;archived:boolean}
export interface PatientRow extends Partial<Owner>,Partial<Pet>{id:number}
export interface Vet extends Base {name:string;specialty:string|null;licenseNo:string|null;workHours:string|null;active:boolean}
export interface Medicine extends Base {name:string;activeIngredient:string|null;form:string;unit:string;stockQuantity:number;reorderLevel:number;unitPrice:number;active:boolean;description:string|null}
export interface VaccineType extends Base {name:string;manufacturer:string|null;description:string|null;active:boolean}
export interface Vaccination extends Base {petId:number;petName:string;vaccineType:string;vaccineId:number|null;administeredAt:string;lotNumber:string|null;nextDueDate:string|null;administeredBy:string|null;notes:string|null}
export interface Visit extends Base {petId:number;petName:string;vetId:number;vetName:string;scheduledAt:string;status:VisitStatus;chiefComplaint:string|null;symptoms:string|null;diagnosis:string|null;treatmentNotes:string|null;followUpDate:string|null;warnings:string[]}
export interface Weight {id:number;petId:number;weightKg:number;recordedAt:string;note:string|null}
export interface PrescriptionItem {id:number;medicineId:number;medicineName:string;unit:string;dosage:string;frequency:string;durationDays:number;quantity:number;unitPrice:number;instructions:string|null}
export interface Prescription {id:number;visitId:number;petName:string;notes:string|null;items:PrescriptionItem[];prescribedAt:string}
export interface InvoiceItem {id:number;description:string;category:InvoiceCategory;quantity:number;unitPrice:number;lineTotal:number}
export interface Invoice {id:number;visitId:number;petId:number;petName:string;issuedAt:string;subtotal:number;vatRate:number;vatAmount:number;total:number;status:InvoiceStatus;items:InvoiceItem[]}
export interface Page<T> {content:T[];total:number;pages:number;page:number}
export interface SupportRequest extends Base {requestedBy:string;subject:string;description:string;status:'OPEN'|'IN_PROGRESS'|'RESOLVED'}
export interface KnowledgeDocument extends Base {title:string;content:string;source:string|null}
export interface Summary {ownerCount:number;petCount:number;inactivePatientCount:number;todaySchedule:Visit[];paidRevenue:number;unpaidCount:number;revenueByMonth:Record<string,number>;vetWorkload:Record<string,number>;appointmentTrend:Record<string,number>;vaccinationAlerts:Vaccination[];followUps:Visit[];lowStock:Medicine[]}
export interface Performance {visitCount:number;completed:number;paidRevenue:number}
export interface SearchRow {id:number;name?:string;petName?:string;firstName?:string;lastName?:string;phone?:string|null;ownerName?:string;diagnosis?:string|null;email?:string|null}
export type SearchResults=Record<'owners'|'pets'|'visits',SearchRow[]>;
export interface NotificationRow {id:number;petId?:number;petName?:string;name?:string;scheduledAt?:string;nextDueDate?:string|null;followUpDate?:string|null;stockQuantity?:number;unit?:string}
export type NotificationsData=Record<'appointments'|'vaccinations'|'followUps'|'lowStock',NotificationRow[]>;
