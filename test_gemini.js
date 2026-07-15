const axios = require('axios');

const apiKey = 'AIzaSyBEYwjs3Js3Mq0HNZ2tNKlJqGllHQpD8I4';
const model = 'gemini-1.5-flash'; // Using 1.5 flash as a common one

async function testGemini() {
    try {
        const url = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${apiKey}`;
        const data = {
            contents: [{
                parts: [{ text: 'Hello, this is a test.' }]
            }]
        };
        const response = await axios.post(url, data, {
            headers: { 'Content-Type': 'application/json' }
        });
        console.log('Success:', JSON.stringify(response.data, null, 2));
    } catch (error) {
        console.error('Error:', error.response ? error.response.status : error.message);
        if (error.response) {
            console.error('Data:', JSON.stringify(error.response.data, null, 2));
        }
    }
}

testGemini();
