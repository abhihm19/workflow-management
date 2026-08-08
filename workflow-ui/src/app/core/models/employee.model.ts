export interface Employee {
  id: number;
  name: string;
  email: string;
  reportingManagerId: number | null;
  dateOfJoining: string | null;
}

export interface CreateEmployeeRequest {
  name: string;
  email: string;
  reportingManagerId: number | null;
  dateOfJoining: string;
}
