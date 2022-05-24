import { createSlice } from "@reduxjs/toolkit";
import type { PayloadAction } from "@reduxjs/toolkit";
import { AuthState, SetAccessLevel } from "redux/types/stateTypes";
import { AccessLevel } from "types/AccessLevel";

const initialState: AuthState = {
   username: "",
   roles: [],
   accessLevel: AccessLevel.GUEST,
   token: "",
   exp: 0,
};

export const authSlice = createSlice({
   name: "auth",
   initialState,
   reducers: {
      login: (state: AuthState, action: PayloadAction<AuthState>) => {
         state.username = action.payload.username;
         state.roles = action.payload.roles;
         state.accessLevel = action.payload.accessLevel;
         state.token = action.payload.token;
         state.exp = action.payload.exp;

         const selectedAccessLevel = localStorage.getItem("accessLevel");

         if (
            selectedAccessLevel === AccessLevel.GUEST ||
            state.roles.includes(selectedAccessLevel as AccessLevel)
         ) {
            localStorage.setItem("accessLevel", state.accessLevel);
         }
      },
      logout: (state: AuthState) => {
         state.username = initialState.username;
         state.roles = initialState.roles;
         state.accessLevel = initialState.accessLevel;
         state.token = initialState.token;
         state.exp = initialState.exp;

         localStorage.setItem("accessLevel", AccessLevel.GUEST);
         localStorage.removeItem("token");
      },
      setAccessLevel: (state: AuthState, action: PayloadAction<SetAccessLevel>) => {
         state.accessLevel = action.payload.accessLevel;
         localStorage.setItem("accessLevel", action.payload.accessLevel);
      },
   },
});

export const { login, logout, setAccessLevel } = authSlice.actions;

export default authSlice.reducer;
