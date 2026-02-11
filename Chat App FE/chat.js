let stompClient = null;
let username = null;

const connectBtn = document.getElementById("connectBtn");
const sendBtn = document.getElementById("sendBtn");
const usernameInput = document.getElementById("usernameInput");
const messageInput = document.getElementById("messageInput");
const messagesDiv = document.getElementById("messages");

connectBtn.addEventListener("click", connect);
sendBtn.addEventListener("click", sendMessage);

function connect() {

    const enteredUsername = usernameInput.value.trim();

    if (!enteredUsername) {
        usernameInput.classList.add("input-error");
        document.getElementById("usernameError").innerText =
            "Username is required";
        return;
    }

    usernameInput.classList.remove("input-error");
    document.getElementById("usernameError").innerText = "";

    username = enteredUsername;

    const socket = new SockJS("http://localhost:8080/ws");
    stompClient = Stomp.over(socket);
    stompClient.debug = null;

    stompClient.connect(
        { username: username },
        onConnected
    );
}

function onConnected() {

    stompClient.subscribe("/topic/chat", (message) => {
        const msg = JSON.parse(message.body);
        showMessage(msg);
    });

    // send join message
    stompClient.send(
        "/app/chat",
        {},
        JSON.stringify({
            type: "JOIN",
            from: username,
            content: "joined the chat"
        })
    );

    usernameInput.disabled = true;
    connectBtn.disabled = true;

    messageInput.disabled = false;
    sendBtn.disabled = false;
}


function sendMessage() {

    const content = messageInput.value.trim();
    if (!content) return;

    const msg = {
        type: "CHAT",
        from: username,
        content: content
    };

    stompClient.send(
        "/app/chat",
        {},
        JSON.stringify(msg)
    );

    messageInput.value = "";
}

function showMessage(msg) {

    const messageElement = document.createElement("div");

    if (msg.type === "JOIN" || msg.type === "LEAVE") {
        messageElement.classList.add("system-message");
        messageElement.innerHTML =
            `<em>[${msg.timestamp}] ${msg.from} ${msg.content}</em>`;
    } else {
        messageElement.classList.add("message");
        messageElement.innerHTML =
            `<strong>${msg.from}</strong>: ${msg.content}
             <span class="timestamp">${msg.timestamp}</span>`;
    }

    messagesDiv.appendChild(messageElement);
    messagesDiv.scrollTop = messagesDiv.scrollHeight;
}

