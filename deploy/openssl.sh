openssl genpkey -algorithm RSA -out key.pem
openssl req -new -key key.pem -out csr.pem -subj "/C=IN/ST=Karnataka/L=Bengaluru/O=siemens/CN=Advanta"
openssl x509 -req -days 365 -in csr.pem -signkey key.pem -out cert.pem