const emailInput = document.getElementById('emailInput');
const emailError = document.getElementById('emailError');
const submitBtn = document.getElementById('submitBtn');
const registerForm = document.getElementById('registerForm');

// Real-time email validation
emailInput.addEventListener('input', function() {
    const email = this.value.trim();
    
    if (!email) {
        emailError.textContent = '';
        submitBtn.disabled = false;
        return;
    }
    
    if (!email.includes('@')) {
        emailError.textContent = '❌ Email must contain @';
        submitBtn.disabled = true;
    } else if (!email.endsWith('@usc.edu')) {
        emailError.textContent = '❌ Email must end with @usc.edu (format: ***@usc.edu)';
        submitBtn.disabled = true;
    } else {
        emailError.textContent = '✓ Valid USC email';
        emailError.style.color = '#00aa00';
        submitBtn.disabled = false;
    }
});

// Form submission
registerForm.addEventListener('submit', function(e) {
    e.preventDefault();
    
    const email = emailInput.value;
    
    // Final validation before submit
    if (!email.endsWith('@usc.edu')) {
        document.getElementById('error').textContent = 'Please use a valid USC email (@usc.edu)';
        return;
    }
    
    const params = new URLSearchParams(new FormData(this));
    
    console.log("Sending registration with:", {
        username: params.get('username'),
        email: params.get('email'),
        passwordLength: params.get('password').length,
        interests: params.get('interests'),
        skillLevel: params.get('skillLevel')
    });
    
    fetch('/CampusActivities/register', { method: 'POST', body: params })
        .then(res => {
            console.log("Response status:", res.status);
            return res.text().then(text => {
                console.log("Response text:", text);
                try {
                    return JSON.parse(text);
                } catch (e) {
                    console.error("Failed to parse JSON:", text);
                    throw new Error("Server returned invalid JSON: " + text.substring(0, 100));
                }
            });
        })
        .then(data => {
            console.log("Response data:", data);
            if (data.success) {
                window.location.href = 'login.html';
            } else {
                document.getElementById('error').textContent = data.message;
            }
        })
        .catch(err => {
            console.error("Error:", err);
            document.getElementById('error').textContent = 'Error: ' + err.message;
        });
});
