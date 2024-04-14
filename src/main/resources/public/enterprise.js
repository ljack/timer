document.addEventListener("DOMContentLoaded", function() {
    // Set up drag and drop listeners for lists
    setupDragListeners();

    // Populate default roles
    addDefaultRoles();
});

function setupDragListeners() {
    const lists = ['userList', 'roleList', 'inactiveList'];
    lists.forEach(listId => {
        const list = document.getElementById(listId);
        list.addEventListener('dragover', function(event) {
            event.preventDefault();
        });
        list.addEventListener('drop', function(event) {
            event.preventDefault();
            const draggingElement = document.querySelector('.dragging');
            if (draggingElement.classList.contains('roleBox') && this.id === 'userList') {
                handleRoleAssignment(draggingElement, event.target);
            } else if (draggingElement.classList.contains('userBox') && this.id === 'userList') {
                this.insertBefore(draggingElement, getClosestDraggableElement(event.clientY, this));
            } else {
                this.appendChild(draggingElement);
            }
            draggingElement.classList.remove('dragging');
        });
    });
}

function handleRoleAssignment(roleBox, targetUser) {
    if (targetUser && targetUser.classList.contains('userBox')) {
        targetUser.style.backgroundColor = roleBox.style.backgroundColor;
        targetUser.dataset.role = roleBox.textContent;
        targetUser.innerHTML += `<span class="badge bg-secondary">${roleBox.textContent}</span>`;
    }
}

function getClosestDraggableElement(y, container) {
    const draggableElements = [...container.querySelectorAll('.draggable:not(.dragging)')];
    return draggableElements.reduce((closest, child) => {
        const box = child.getBoundingClientRect();
        const offset = y - box.top - box.height / 2;
        if (offset < 0 && offset > closest.offset) {
            return { offset: offset, element: child };
        } else {
            return closest;
        }
    }, { offset: Number.NEGATIVE_INFINITY }).element;
}

function createUserBox(userName) {
    const userBox = document.createElement('div');
    userBox.className = 'userBox draggable';
    userBox.draggable = true;
    userBox.textContent = userName;
    userBox.addEventListener('dragstart', () => userBox.classList.add('dragging'));
    userBox.addEventListener('dragend', () => userBox.classList.remove('dragging'));
    return userBox;
}

function createRoleBox(roleName, color) {
    const roleBox = document.createElement('div');
    roleBox.className = 'roleBox draggable';
    roleBox.draggable = true;
    roleBox.textContent = roleName;
    roleBox.style.backgroundColor = color;
    roleBox.addEventListener('dragstart', () => roleBox.classList.add('dragging'));
    roleBox.addEventListener('dragend', () => roleBox.classList.remove('dragging'));
    return roleBox;
}

function addDefaultRoles() {
    const roles = [
        { name: "Typist", color: "#6f42c1" },
        { name: "Driver", color: "#dc3545" },
        { name: "Facilitator", color: "#28a745" }
    ];
    roles.forEach(role => {
        const roleBox = createRoleBox(role.name, role.color);
        document.getElementById('roleList').appendChild(roleBox);
    });
}
