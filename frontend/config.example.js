// Only needed if the frontend is deployed separately from the backend
// (e.g. this site on Vercel/Netlify, the Spring Boot API on Render/Railway).
//
// Set this to your deployed backend's base URL, then add a <script
// src="config.js"></script> tag in index.html BEFORE the main inline
// <script> block. If frontend and backend share an origin, delete this
// file and the <script> tag - index.html already defaults to same-origin.
window.VITAL_AIR_API_BASE = "https://vital-air-backend.onrender.com";
