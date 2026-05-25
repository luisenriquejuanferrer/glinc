export interface UserProfile {
  email: string;
  firstName: string | null;
  lastName: string | null;
  birthDate: string | null;
  phone: string | null;
  updatedAt: string;
}

export interface UpdateUserProfileRequest {
  firstName: string | null;
  lastName: string | null;
  birthDate: string | null;
  phone: string | null;
}
