document.addEventListener('DOMContentLoaded', async () => {
    try {
        // 1. Загружаем HTML хедера
        const response = await fetch('/partials/header.html');
        if (!response.ok) throw new Error('Не удалось загрузить хедер: ' + response.status);

        const headerHtml = await response.text();

        // 2. Вставляем в начало <body>
        document.body.insertAdjacentHTML('afterbegin', headerHtml);
        console.log('✅ Хедер загружен');

        // 3. Инициализируем логику ПОСЛЕ вставки в DOM
        initHeaderLogic();

    } catch (error) {
        console.error('❌ Ошибка загрузки хедера:', error);
    }
});

// === Логика хедера (вызывается после загрузки) ===
function initHeaderLogic() {
    // Делегирование кликов (работает для динамического контента)
    document.addEventListener('click', (e) => {
        // Логотип
        if (e.target.closest('#logoBtn') || e.target.closest('.logo')) {
            e.preventDefault();
            window.location.href = '/';
            return;
        }

        // Каталог
        if (e.target.closest('#catalogBtn')) {
            e.preventDefault();
            if (typeof window.openCatalogModal === 'function') {
                window.openCatalogModal();
            } else {
                document.getElementById('categoryModal')?.classList.add('show');
            }
            return;
        }

        // Адрес доставки
        if (e.target.closest('#addressPicker')) {
            e.preventDefault();
            if (typeof window.openChoiceModal === 'function') {
                window.openChoiceModal();
            } else {
                const modal = document.getElementById('addressChoiceModal');
                if (modal) {
                    modal.classList.add('show');
                    if (typeof window.setActiveChoice === 'function') {
                        window.setActiveChoice('courier');
                    }
                }
            }
            return;
        }
    });

    // Поиск: клик по лупе
    const searchBtn = document.getElementById('searchBtn');
    const searchInput = document.getElementById('searchInput');
    if (searchBtn && searchInput) {
        searchBtn.addEventListener('click', () => {
            const query = searchInput.value.trim();
            if (query) window.location.href = `/search.html?q=${encodeURIComponent(query)}`;
        });
        searchInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') searchBtn.click();
        });
    }

    // Загрузка профиля
    loadUserInfo();
}

// === Загрузка данных пользователя ===
async function loadUserInfo() {
    const token = localStorage.getItem('jwt');
    const userInfoDiv = document.getElementById('userInfo');
    if (!userInfoDiv) return;

    if (!token) {
        userInfoDiv.innerHTML = '<a href="/login" class="icon-btn"><span class="icon-label">Вход</span></a> <a href="/register" class="icon-btn"><span class="icon-label">Регистрация</span></a>';
        return;
    }

    try {
        const response = await fetch('/api/users/profile', {
            headers: { 'Authorization': 'Bearer ' + token }
        });

        if (response.ok) {
            const user = await response.json();
            const isAdmin = user.roles && user.roles.includes('ROLE_ADMIN');
            let html = `<a href="/cart-icon2.html" class="icon-btn"><img src="/images/cart-icon2.png" class="cart-icon2"><span class="icon-label">Корзина</span></a>
                        <a href="/profile-icon2.html" class="icon-btn"><img src="/images/profile-icon2.png" class="profile-icon2"><span class="icon-label">Профиль</span></a>`;
            if (isAdmin) html += '<span class="admin-badge">ADMIN</span>';
            html += '<button class="logout-btn" id="logoutBtn">Выйти</button>';
            userInfoDiv.innerHTML = html;

            document.getElementById('logoutBtn')?.addEventListener('click', () => {
                localStorage.removeItem('jwt');
                window.location.reload();
            });
        } else {
            localStorage.removeItem('jwt');
            userInfoDiv.innerHTML = '<a href="/login">Вход</a> <a href="/register">Регистрация</a>';
        }
    } catch (err) {
        console.error('Ошибка загрузки профиля', err);
    }
}