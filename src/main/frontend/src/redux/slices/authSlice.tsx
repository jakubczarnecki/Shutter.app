import { createSlice } from "@reduxjs/toolkit";
import { AuthState } from "redux/types/stateTypes";
import { AccessLevel } from "types/AccessLevel";

const initialState: AuthState = {
   username: "",
   accessLevel: [AccessLevel.GUEST],
};

export const authSlice = createSlice({
   name: "auth",
   initialState,
   reducers: {
      login: (state: AuthState, action: any) => {
         state.username = action.payload.username;
         state.accessLevel = action.payload.accessLevel;
      },
      logout: (state) => {
         return initialState;
      },
   },
});

export const { login, logout } = authSlice.actions;

export default authSlice.reducer;
