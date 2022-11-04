package org.openhab.binding.enphase.internal.dto;

public class EntrezJwtDTO {

    public class EntrezJwtHeaderDTO {
        private String kid;
        private String typ;
        private String alg;

        public String getKid() {
            return kid;
        }

        public void setKid(String kid) {
            this.kid = kid;
        }

        public String getTyp() {
            return typ;
        }

        public void setTyp(String typ) {
            this.typ = typ;
        }

        public String getAlg() {
            return alg;
        }

        public void setAlg(String alg) {
            this.alg = alg;
        }

    }

    public class EntrezJwtBodyDTO {
        private String aud;
        private String iss;
        private String enphaseUser;
        private Long exp;
        private Long iat;
        private String jti;
        private String username;

        public String getAud() {
            return aud;
        }

        public void setAud(String aud) {
            this.aud = aud;
        }

        public String getIss() {
            return iss;
        }

        public void setIss(String iss) {
            this.iss = iss;
        }

        public String getEnphaseUser() {
            return enphaseUser;
        }

        public void setEnphaseUser(String enphaseUser) {
            this.enphaseUser = enphaseUser;
        }

        public Long getExp() {
            return exp;
        }

        public void setExp(Long exp) {
            this.exp = exp;
        }

        public Long getIat() {
            return iat;
        }

        public void setIat(Long iat) {
            this.iat = iat;
        }

        public String getJti() {
            return jti;
        }

        public void setJti(String jti) {
            this.jti = jti;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

    }

}
