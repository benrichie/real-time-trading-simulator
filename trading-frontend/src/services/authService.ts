// src/services/authService.ts
import apiClient from '../api/client';
import { LoginRequest, RegisterRequest } from '../types';

export const authService = {
  async login(credentials: LoginRequest) {
    const response = await apiClient.post('/auth/login', credentials);
    const { accessToken } = response.data;

    localStorage.setItem('token', accessToken);
    localStorage.setItem('user', JSON.stringify({ username: credentials.username }));

    return { token: accessToken, username: credentials.username };
  },

  async register(userData: RegisterRequest) {
    const response = await apiClient.post('/auth/register', userData);
    return response.data; // { accessToken: ... }
  },

  logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    window.location.href = '/login';
  },

  getToken() {
    return localStorage.getItem('token');
  },

  getCurrentUser() {
    const userStr = localStorage.getItem('user');
    return userStr ? JSON.parse(userStr) : null;
  },
};
