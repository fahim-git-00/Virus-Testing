pub fn lab_status() -> &'static str {
    "Rust security lab core is ready."
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn status_is_available() {
        assert!(!lab_status().is_empty());
    }
}
