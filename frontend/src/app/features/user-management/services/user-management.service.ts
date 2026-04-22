import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { ApiResponse } from '../../../core/models/api-response.model';
import { User, UserCreateRequest, UserUpdateRequest } from '../../../core/models/user.model';

@Injectable({
  providedIn: 'root'
})
export class UserManagementService {
  private readonly apiBaseUrl = environment.apiBaseUrl;

  constructor(private readonly httpClient: HttpClient) {}

  getUsers(): Observable<User[]> {
    return this.httpClient
      .get<ApiResponse<{ content: User[] }>>(`${this.apiBaseUrl}/api/users`)
      .pipe(map((response) => response.data.content));
  }

  createUser(payload: UserCreateRequest): Observable<User> {
    return this.httpClient
      .post<ApiResponse<User>>(`${this.apiBaseUrl}/api/users`, payload)
      .pipe(map((response) => response.data));
  }

  updateUser(userId: string, payload: UserUpdateRequest): Observable<User> {
    return this.httpClient
      .put<ApiResponse<User>>(`${this.apiBaseUrl}/api/users/${userId}`, payload)
      .pipe(map((response) => response.data));
  }

  deleteUser(userId: string): Observable<User> {
    return this.httpClient
      .delete<ApiResponse<User>>(`${this.apiBaseUrl}/api/users/${userId}`)
      .pipe(map((response) => response.data));
  }
}
