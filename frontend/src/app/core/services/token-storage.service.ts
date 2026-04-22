import { Injectable } from '@angular/core';

const ACCESS_TOKEN_KEY = 'prp_access_token';
const USER_ROLES_KEY = 'prp_user_roles';

@Injectable({
  providedIn: 'root'
})
export class TokenStorageService {
  private accessTokenInMemory: string | null = null;
  private rolesInMemory: string[] = [];

  saveToken(token: string): void {
    this.accessTokenInMemory = token;
    sessionStorage.setItem(ACCESS_TOKEN_KEY, token);
  }

  getToken(): string | null {
    if (this.accessTokenInMemory) {
      return this.accessTokenInMemory;
    }
    this.accessTokenInMemory = sessionStorage.getItem(ACCESS_TOKEN_KEY);
    return this.accessTokenInMemory;
  }

  saveRoles(roles: string[]): void {
    this.rolesInMemory = [...roles];
    sessionStorage.setItem(USER_ROLES_KEY, JSON.stringify(roles));
  }

  getRoles(): string[] {
    if (this.rolesInMemory.length > 0) {
      return [...this.rolesInMemory];
    }
    const serialized = sessionStorage.getItem(USER_ROLES_KEY);
    if (!serialized) {
      return [];
    }
    try {
      this.rolesInMemory = JSON.parse(serialized) as string[];
      return [...this.rolesInMemory];
    } catch {
      this.rolesInMemory = [];
      return [];
    }
  }

  clear(): void {
    this.accessTokenInMemory = null;
    this.rolesInMemory = [];
    sessionStorage.removeItem(ACCESS_TOKEN_KEY);
    sessionStorage.removeItem(USER_ROLES_KEY);
  }
}
