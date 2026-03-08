# Claude Code Privacy Guide

This document outlines privacy and security practices when using Claude Code with this project.

## 🔒 Pre-Session Checklist

### Before Each Claude Session:
- [ ] Verify `.claudeignore` is up to date
- [ ] Check no sensitive files in working directory
- [ ] Sanitize any code you plan to share
- [ ] Use generic examples instead of real data

## 🧹 Code Sanitization Templates

### 1. Package Name Sanitization

**Before (Sensitive):**
```java
package com.acmecorp.banking.fraud.detection;
package com.clientname.healthcare.patient.records;
```

**After (Sanitized):**
```java
package com.enterprise.financial.security;
package com.healthcare.patient.management;
```

### 2. Configuration Sanitization

**Before (Sensitive):**
```properties
# application-prod.properties
acme.database.url=jdbc:postgresql://prod-db.acmecorp.internal:5432/banking
acme.api.key=sk_live_xyz123
client.name=ACME Corporation
customer.id=ACME_BANK_001
```

**After (Sanitized):**
```properties
# application-example.properties
app.database.url=jdbc:postgresql://localhost:5432/database
app.api.key=${API_KEY}
client.name=${CLIENT_NAME}
customer.id=${CUSTOMER_ID}
```

### 3. Class and Method Names

**Before (Sensitive):**
```java
public class AcmeCorpFraudDetectionService {
    public boolean detectSuspiciousTransaction(AcmeTransaction tx) {
        // Implementation
    }
}
```

**After (Sanitized):**
```java
public class FraudDetectionService {
    public boolean detectSuspiciousTransaction(Transaction tx) {
        // Implementation
    }
}
```

### 4. URLs and Endpoints

**Before (Sensitive):**
```java
@Value("${acme.fraud.api.url:https://fraud-api.acmecorp.com}")
private String fraudApiUrl;

@PostMapping("/api/acme/fraud/check")
public ResponseEntity<FraudResult> checkFraud(@RequestBody AcmeTransaction tx) {
    // Implementation
}
```

**After (Sanitized):**
```java
@Value("${fraud.api.url:https://api.example.com}")
private String fraudApiUrl;

@PostMapping("/api/fraud/check")
public ResponseEntity<FraudResult> checkFraud(@RequestBody Transaction tx) {
    // Implementation
}
```

### 5. Database Schema References

**Before (Sensitive):**
```sql
SELECT * FROM acme_customers 
JOIN acme_accounts ON acme_customers.id = acme_accounts.customer_id
WHERE acme_customers.risk_level = 'HIGH';
```

**After (Sanitized):**
```sql
SELECT * FROM customers 
JOIN accounts ON customers.id = accounts.customer_id
WHERE customers.risk_level = 'HIGH';
```

## 🚫 Never Share Categories

### Absolute No-Go Items:
- **Real customer names** or identifiers
- **Production URLs** or IP addresses
- **API keys, tokens, passwords**
- **Database connection strings**
- **Environment files** (.env, .properties)
- **Deployment scripts** with real environments
- **License keys** or certificates
- **Internal architecture diagrams**
- **Business logic** with customer-specific rules

### Conditional Sharing (Sanitized Only):
- **Code patterns** with generic names
- **Configuration templates** with placeholders
- **Test data** with fake values
- **Documentation** with sensitive parts removed

## 📋 Session Management Rules

### Start of Session:
1. State your privacy requirements upfront
2. Mention you're working with sanitized examples
3. Request generic solutions that you'll adapt

### During Session:
- Use placeholders: `${CUSTOMER_NAME}`, `${API_KEY}`
- Reference "the client" instead of specific names
- Focus on patterns, not specific implementations

### End of Session:
- Review shared code for any missed sensitive data
- Clear conversation if sensitive info was accidentally shared

## 🔍 Quick Sanitization Script

Create this utility for fast sanitization:

```bash
#!/bin/bash
# sanitize-for-claude.sh

# Replace sensitive patterns in copied code
sed -i '' 's/acmecorp/enterprise/g' "$1"
sed -i '' 's/ACME/CLIENT/g' "$1"
sed -i '' 's/banking/financial/g' "$1"
sed -i '' 's/fraud-api\.acmecorp\.com/api.example.com/g' "$1"
# Add more patterns as needed
```

## 🚨 Incident Response

If sensitive data is accidentally shared:
1. **Stop the conversation** immediately
2. **Document what was shared**
3. **Start a new session** for future work
4. **Review and update** sanitization practices
5. **Report to security team** if required

## 📞 Escalation Contacts

- **Security Team**: [security@company.com]
- **Privacy Officer**: [privacy@company.com]
- **Project Lead**: [lead@company.com]

---

**Remember**: When in doubt, don't share. It's better to work with generic examples than risk exposing sensitive information.