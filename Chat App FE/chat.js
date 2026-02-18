let stompClient = null;
let username = null;
let currentRoom = null;
let currentRoomSubscription = null;
let currentUsersSubscription = null;
let activePrivateUser = null;
let privateChats = {};
let unreadPrivate = {};

const connectBtn = document.getElementById("connectBtn");
const sendBtn = document.getElementById("sendBtn");
const usernameInput = document.getElementById("usernameInput");
const messageInput = document.getElementById("messageInput");
const privateMessageInput = document.getElementById("privateMessageInput");
const messagesDiv = document.getElementById("messages");
const usersList = document.getElementById("usersList");
const roomsList = document.getElementById("roomsList");
const roomInput = document.getElementById("roomInput");

connectBtn.addEventListener("click", connect);
sendBtn.addEventListener("click", sendMessage);
messageInput.addEventListener("keydown", function (event) {
    if (event.key === "Enter") {
        event.preventDefault();
        sendMessage();
    }
});
privateMessageInput.addEventListener("keydown", function (event) {
    if (event.key === "Enter") {
        event.preventDefault();
        sendPrivateMessage();
    }
});

let reconnectAttempts = 0;
const maxReconnectAttempts = 5;
const baseReconnectDelay = 2000;

function connect() {
    const enteredUsername = usernameInput.value.trim();
    if (!enteredUsername) {
        usernameInput.classList.add("input-error");
        document.getElementById("usernameError").innerText = "Username required";
        return;
    }

    usernameInput.classList.remove("input-error");
    document.getElementById("usernameError").innerText = "";

    username = enteredUsername;
    currentRoom = roomInput.value.trim() || "general";

    const socket = new SockJS("http://localhost:8080/ws");
    stompClient = Stomp.over(socket);
    stompClient.debug = null;

    showConnectionStatus("disconnected", "Connecting...");
    disableInputs();

    stompClient.connect(
        { username: username },
        () => {
            onConnected();
            reconnectAttempts = 0;
            showConnectionStatus("connected", "Connected");
            enableInputs();
        },
        (error) => {
            console.error("WebSocket disconnected:", error);
            showConnectionStatus("disconnected", "Disconnected – reconnecting...");

            if (reconnectAttempts < maxReconnectAttempts) {
                reconnectAttempts++;
                const delay = baseReconnectDelay * Math.pow(1.5, reconnectAttempts);
                console.log(`Reconnecting in ${Math.round(delay / 1000)}s (attempt ${reconnectAttempts}/${maxReconnectAttempts})`);
                setTimeout(connect, delay);
            } else {
                showConnectionStatus("disconnected", "Connection lost – refresh page");
                alert("Could not reconnect to the server. Please refresh the page.");
                reconnectAttempts = 0;
            }
        }
    );
}

function onConnected() {
    // Subscribe to history 
    stompClient.subscribe("/user/queue/history", (msg) => {
        const history = JSON.parse(msg.body);
        messagesDiv.innerHTML = "";
        history.forEach(m => showMessage(m));
    });

    stompClient.subscribe("/user/queue/private-history", (msg) => {
        const history = JSON.parse(msg.body);

        privateChats[activePrivateUser] = history;
        renderPrivateMessages(activePrivateUser);
    });

    // Subscribe to rooms list
    stompClient.subscribe("/topic/rooms", (msg) => updateRooms(msg));

    // Private messages
    stompClient.subscribe("/user/queue/private", (msg) => {

        const message = JSON.parse(msg.body);

        const otherUser = message.from === username
            ? message.to
            : message.from;

        if (!privateChats[otherUser])
            privateChats[otherUser] = [];

        privateChats[otherUser].push(message);

        if (activePrivateUser !== otherUser) {
            unreadPrivate[otherUser] = true;
            renderUsersList();
        } else {
            renderPrivateMessages(otherUser);
        }
    });

    if (currentRoom) {
        subscribeRoom(currentRoom);
    }

    usernameInput.disabled = true;
    connectBtn.disabled = true;
    roomInput.disabled = true;
    messageInput.disabled = false;
    sendBtn.disabled = false;
    messageInput.focus();
}

function subscribeRoom(room) {
    if (room === currentRoom && currentRoomSubscription) {
        return;
    }

    if (currentRoomSubscription) {
        stompClient.send(
            "/app/chat/leave",
            {},
            JSON.stringify({ room: currentRoom })
        );

        currentRoomSubscription.unsubscribe();
    }

    if (currentUsersSubscription) {
        currentUsersSubscription.unsubscribe();
    }

    currentRoom = room;

    // Subscribe to messages
    currentRoomSubscription = stompClient.subscribe(
        "/topic/room/" + room,
        (msg) => showMessage(JSON.parse(msg.body))
    );

    // Subscribe to users
    currentUsersSubscription = stompClient.subscribe(
        "/topic/users/" + room,
        (msg) => updateUsers(msg)
    );

    stompClient.send(
        "/app/chat/join",
        {},
        JSON.stringify({ room: room })
    );
}

function sendMessage() {
    if (!stompClient || !stompClient.connected) {
        console.warn("Cannot send – not connected");
        return;
    }

    const content = messageInput.value.trim();
    if (!content) return;

    const msg = {
        type: "CHAT",
        room: currentRoom,
        content: content
    };

    stompClient.send("/app/chat/send", {}, JSON.stringify(msg));
    messageInput.value = "";
}

function showMessage(msg, isPrivate = false) {

    const messageElement = document.createElement("div");

    if (msg.type === "JOIN" || msg.type === "LEAVE") {
        messageElement.classList.add("system-message");
        messageElement.innerHTML =
            `<em>[${msg.timestamp}] ${msg.from} ${msg.content}</em>`;
    } else {
        messageElement.classList.add("message");
        messageElement.classList.add(
            msg.from === username ? "my-message" : "other-message"
        );

        const privateTag = isPrivate ? " (private)" : "";

        messageElement.innerHTML = `
    <div class="message-bubble">
        <div class="message-text"><strong>${msg.from}:</strong> ${msg.content}</div>
        <div class="timestamp">${msg.timestamp}</div>
    </div>
`;
    }

    messagesDiv.appendChild(messageElement);
    messagesDiv.scrollTop = messagesDiv.scrollHeight;
}

let currentUsers = [];

function updateUsers(msg) {
    currentUsers = JSON.parse(msg.body);
    renderUsersList();
}

function renderUsersList() {

    usersList.innerHTML = "";

    currentUsers.forEach(user => {

        const li = document.createElement("li");

        if (user === username) {
            li.innerText = user;
        } else {

            li.innerHTML = user;

            if (unreadPrivate[user]) {
                const dot = document.createElement("span");
                dot.classList.add("notification-dot");
                li.appendChild(dot);
            }

            li.onclick = () => {
                unreadPrivate[user] = false;
                openPrivateChat(user);
                renderUsersList();
            };
        }

        usersList.appendChild(li);
    });
}

function updateRooms(msg) {
    const rooms = JSON.parse(msg.body);

    roomsList.innerHTML = "";

    rooms.forEach(r => {
        const li = document.createElement("li");
        li.innerText = r;

        li.onclick = () => subscribeRoom(r);

        roomsList.appendChild(li);
    });
}

function openPrivateChat(user) {
    activePrivateUser = user;
    document.getElementById("privateChatTitle").innerText = "Private chat with " + user;
    document.getElementById("privateChatPanel").style.display = "flex";

    renderPrivateMessages(user);

    setTimeout(() => {
        document.getElementById("privateMessageInput").focus();
    }, 50);
}

function closePrivateChat() {
    document.getElementById("privateChatPanel").style.display = "none";
    activePrivateUser = null;
}

function renderPrivateMessages(user) {
    const container = document.getElementById("privateMessages");
    container.innerHTML = "";

    const messages = privateChats[user] || [];

    messages.forEach(msg => {
        const isOwn = msg.from === username;

        const messageEl = document.createElement("div");
        messageEl.classList.add("message");
        messageEl.classList.add(isOwn ? "my-message" : "other-message");

        const bubble = document.createElement("div");
        bubble.classList.add("message-bubble");

        const text = document.createElement("div");
        text.textContent = msg.content;

        const time = document.createElement("div");
        time.classList.add("timestamp");
        time.textContent = msg.timestamp;

        bubble.appendChild(text);
        bubble.appendChild(time);

        messageEl.appendChild(bubble);
        container.appendChild(messageEl);
    });

    container.scrollTop = container.scrollHeight;
}

function sendPrivateMessage() {
    if (!stompClient || !stompClient.connected) {
        console.warn("Cannot send – not connected");
        return;
    }

    const input = document.getElementById("privateMessageInput");
    const content = input.value.trim();

    if (!content || !activePrivateUser) return;

    stompClient.send(
        "/app/chat/send/private",
        {},
        JSON.stringify({
            to: activePrivateUser,
            content: content
        })
    );

    input.value = "";
}

function showConnectionStatus(type, message) {
    const status = document.getElementById("connectionStatus");
    if (!status) return;

    status.textContent = message;
    status.className = `connection-status ${type}`;
    status.classList.remove("hidden");

    if (type === "connected") {
        setTimeout(() => status.classList.add("hidden"), 3000);
    }
}

function disableInputs() {
    messageInput.disabled = true;
    sendBtn.disabled = true;
    if (privateMessageInput) privateMessageInput.disabled = true;
}

function enableInputs() {
    messageInput.disabled = false;
    sendBtn.disabled = false;
    if (privateMessageInput) privateMessageInput.disabled = false;
}

function adjustTimestampPositions() {
    document.querySelectorAll('.message-bubble').forEach(bubble => {
        const ts = bubble.querySelector('.timestamp');
        if (!ts) return;

        const rect = bubble.getBoundingClientRect();
        const container = bubble.closest('.messages, .private-messages');
        if (!container) return;

        const containerRect = container.getBoundingClientRect();
        const spaceAbove = rect.top - containerRect.top;

        if (spaceAbove < 45) {
            ts.classList.add('below');
        } else {
            ts.classList.remove('below');
        }
    });
}

const originalShowMessage = showMessage;
showMessage = function (...args) {
    originalShowMessage.apply(this, args);
    setTimeout(adjustTimestampPositions, 50);
};

const originalRenderPrivate = renderPrivateMessages;
renderPrivateMessages = function (...args) {
    originalRenderPrivate.apply(this, args);
    setTimeout(adjustTimestampPositions, 50);
};

document.getElementById('messages')?.addEventListener('scroll', adjustTimestampPositions);
document.getElementById('privateMessages')?.addEventListener('scroll', adjustTimestampPositions);

setTimeout(adjustTimestampPositions, 300);