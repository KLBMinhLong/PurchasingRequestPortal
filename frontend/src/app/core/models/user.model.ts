export interface User {
  id: string;
  username: string;
  email: string;
  firstName: string | null;
  lastName: string | null;
  phone: string | null;
  active: boolean;
  locked: boolean;
  roleCodes: string[];
  createdAt: string;
  updatedAt: string;
}

export interface UserCreateRequest {
  username: string;
  email: string;
  password: string;
  firstName?: string;
  lastName?: string;
  phone?: string;
  active?: boolean;
  locked?: boolean;
  roleCodes?: string[];
  attributes?: Record<string, unknown>;
}

export interface UserUpdateRequest {
  username?: string;
  email?: string;
  password?: string;
  firstName?: string;
  lastName?: string;
  phone?: string;
  active?: boolean;
  locked?: boolean;
  roleCodes?: string[];
  attributes?: Record<string, unknown>;
}
