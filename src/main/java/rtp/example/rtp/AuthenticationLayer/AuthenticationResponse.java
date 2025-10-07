package rtp.example.rtp.AuthenticationLayer;

public class AuthenticationResponse {
    private String accessToken;

    public AuthenticationResponse() {}

    public AuthenticationResponse(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String accessToken;

        public Builder accessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        public AuthenticationResponse build() {
            return new AuthenticationResponse(accessToken);
        }
    }
}