import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { ConfigProvider } from 'antd';
import { GoogleOAuthProvider } from '@react-oauth/google';
import App from './App';
import './index.css';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <GoogleOAuthProvider clientId="819322140862-2i0jo66s31uj6dk0a0tn5fr9f53ljugu.apps.googleusercontent.com">
      <ConfigProvider
        theme={{
          token: {
            colorPrimary: '#f97316',
            borderRadius: 8,
          },
        }}
      >
        <BrowserRouter>
          <App />
        </BrowserRouter>
      </ConfigProvider>
    </GoogleOAuthProvider>
  </React.StrictMode>
);
