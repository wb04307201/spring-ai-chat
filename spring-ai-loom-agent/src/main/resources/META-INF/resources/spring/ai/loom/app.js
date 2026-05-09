/**
 * app.js — Spring AI LoomAgent Frontend
 * 14-partition modular architecture.
 * §4 API service layer = only fetch() calls.
 * §12 UI components = only DOM manipulation.
 * §2 Global state = only state writes.
 */

// ===================== §1 Constants & Configuration =====================
const API_PREFIX = '';
const API = {
    autoLogin: '/spring/ai/loom/user/isAutoLogin',
    login: '/spring/ai/loom/user/login',
    listConversations: '/spring/ai/loom/conversation',
    getConversation: (id) => `/spring/ai/loom/conversation/${id}`,
    deleteConversation: (id) => `/spring/ai/loom/conversation/${id}`,
    stream: '/spring/ai/loom/stream',
    listMcps: '/spring/ai/chat/loom',
    listSkills: '/spring/ai/chat/skill',
    getSkill: (name) => `/spring/ai/chat/skill/${name}`,
    listKnowledge: '/spring/ai/loom/knowledge',
    createKnowledge: '/spring/ai/loom/knowledge',
    deleteKnowledge: (id) => `/spring/ai/loom/knowledge/${id}`,
    uploadToKnowledge: (id) => `/spring/ai/loom/knowledge/${id}/upload`,
    listKnowledgeFiles: (id) => `/spring/ai/loom/knowledge/${id}/file`,
    deleteKnowledgeFile: (knowledgeId, fileId) => `/spring/ai/loom/knowledge/${knowledgeId}/file/${fileId}`,
    uploadFile: '/spring/ai/loom/file/upload',
    checkKnowledgeUpload: '/spring/ai/loom/knowledge/checkKnowledgeUpload',
    titleMaxLength: 20,
    sseTimeout: 0,
};

// ===================== §2 Global State =====================
const state = {
    username: null,
    token: null,
    conversationId: null,
    selectedMcps: [],
    selectedKnowledgeId: null,
    selectedSkill: null,
    enableRag: false,
    isStreaming: false,
    controller: null, // AbortController for SSE
    mcps: [],
    skills: [],
    currentChatMessageId: null,
    pendingImage: null, // { fileId, objectUrl, fileName }
};

// ===================== §3 Utility Functions =====================

/** Minimal SSE event parser — replaces eventsource-parser dependency */
function createParser(handlers) {
    let buffer = '';
    return {
        feed(chunk) {
            buffer += chunk;
            const lines = buffer.split(/\r?\n/);
            buffer = lines.pop(); // keep incomplete last line
            let data = '';
            for (const line of lines) {
                if (line.startsWith('data:')) {
                    data += line.slice(5).replace(/^\s/, '');
                } else if (line === '' && data) {
                    handlers.onEvent?.({data});
                    data = '';
                }
            }
        }
    };
}

function showToast(message, type = 'success') {
    const toast = document.getElementById('toast-notification');
    toast.textContent = message;
    toast.className = 'show ' + type;
    setTimeout(() => { toast.className = toast.className.replace('show', ''); }, 3000);
}

/** Wrapper for fetch that auto-clears auth on 401 */
async function apiFetch(url, options = {}) {
    const resp = await fetch(url, options);
    if (resp.status === 401 && state.token) {
        // Only clear if there was a token — means token expired or became invalid.
        // Don't clear during initial load when no token has been set yet.
        auth.clear();
    }
    return resp;
}

function formatDate(dateString) {
    if (!dateString) return '未知';
    const date = new Date(dateString);
    return date.toLocaleDateString('zh-CN', {
        year: 'numeric', month: '2-digit', day: '2-digit',
        hour: '2-digit', minute: '2-digit'
    });
}

function formatFileSize(bytes) {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Number.parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

function truncateText(text, maxLength) {
    if (!text) return '';
    if (text.length <= maxLength) return text;
    return text.substring(0, maxLength) + '...';
}

function renderMarkdown(text) {
    try { return marked.parse(text); }
    catch { return text; }
}

function getAuthHeader() {
    const h = {};
    if (state.token) h['Authorization'] = state.token;
    return h;
}

// ===================== §4 API Service Layer =====================
const api = {
    async autoLogin() {
        const r = await fetch(API.autoLogin, { method: 'POST', headers: { 'Content-Type': 'application/json' } });
        return r.ok ? r.json() : null;
    },
    async login(req) {
        const r = await fetch(API.login, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(req),
        });
        return r.ok ? r.json() : null;
    },
    async listConversations() {
        const r = await apiFetch(API.listConversations, {headers: getAuthHeader()});
        return r.ok ? r.json() : [];
    },
    async getConversationMessages(id) {
        const r = await apiFetch(API.getConversation(id), {headers: getAuthHeader()});
        return r.ok ? r.json() : [];
    },
    async deleteConversation(id) {
        const r = await apiFetch(API.deleteConversation(id), {method: 'DELETE', headers: getAuthHeader()});
        return r.ok;
    },
    async listMcps() {
        const r = await apiFetch(API.listMcps, {headers: getAuthHeader()});
        return r.ok ? r.json() : [];
    },
    async listSkills() {
        const r = await apiFetch(API.listSkills, {headers: getAuthHeader()});
        return r.ok ? r.json() : [];
    },
    async listKnowledge() {
        const r = await apiFetch(API.listKnowledge, {headers: getAuthHeader()});
        return r.ok ? r.json() : [];
    },
    async createKnowledge(name) {
        const r = await apiFetch(API.createKnowledge, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json', ...getAuthHeader() },
            body: JSON.stringify({ name }),
        });
        return r.ok ? r.json() : null;
    },
    async deleteKnowledge(id) {
        const r = await apiFetch(API.deleteKnowledge(id), {method: 'DELETE', headers: getAuthHeader()});
        return r.ok ? r.json() : null;
    },
    async listKnowledgeFiles(id) {
        const r = await apiFetch(API.listKnowledgeFiles(id), {headers: getAuthHeader()});
        return r.ok ? r.json() : [];
    },
    async uploadToKnowledge(id, file) {
        const fd = new FormData();
        fd.append('file', file);
        const r = await apiFetch(API.uploadToKnowledge(id), {method: 'POST', body: fd, headers: getAuthHeader()});
        return r.ok ? r.json() : null;
    },
    async deleteKnowledgeFile(knowledgeId, fileId) {
        const r = await apiFetch(API.deleteKnowledgeFile(knowledgeId, fileId), {
            method: 'DELETE',
            headers: getAuthHeader()
        });
        return r.ok ? r.json() : null;
    },
    async uploadFile(file) {
        const fd = new FormData();
        fd.append('file', file);
        const r = await apiFetch(API.uploadFile, {method: 'POST', body: fd, headers: getAuthHeader()});
        return r.ok ? r.json() : null;
    },
    async uploadImage(file) {
        const data = await api.uploadFile(file);
        if (data && data.fileId) {
            return { fileId: data.fileId, status: data.status };
        }
        throw new Error('上传失败：未返回 fileId');
    },
    async checkKnowledgeUpload() {
        const r = await apiFetch(API.checkKnowledgeUpload, {headers: getAuthHeader()});
        return r.ok;
    },
    async streamChat(record, onChunk, onComplete, onError) {
        const resp = await apiFetch(API.stream, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', ...getAuthHeader() },
            body: JSON.stringify(record),
        });
        if (!resp.ok) throw new Error(`HTTP ${resp.status}`);

        const reader = resp.body.getReader();
        const decoder = new TextDecoder();
        const parser = createParser({
            onEvent: (event) => {
                const data = JSON.parse(event.data);
                onChunk(data);
            }
        });

        async function read() {
            const { done, value } = await reader.read();
            if (done) { onComplete(); return; }
            parser.feed(decoder.decode(value, { stream: true }));
            await read();
        }
        await read();
    },
};

// ===================== §5 Auth Module =====================
const auth = {
    STORAGE_KEY_TOKEN: 'loomAgentToken',
    STORAGE_KEY_NICKNAME: 'loomAgentNickname',

    /** Initialize auth state on page load.
     *  1. If token exists in localStorage → restore session
     *  2. Else call isAutoLogin → if true, auto-login with empty fields
     *  3. Else show "未登录"
     */
    async init() {
        // Try to restore from localStorage first
        const savedToken = localStorage.getItem(this.STORAGE_KEY_TOKEN);
        const savedNickname = localStorage.getItem(this.STORAGE_KEY_NICKNAME);
        if (savedToken && savedNickname) {
            state.token = savedToken;
            state.username = savedNickname;
            this.renderUserState(savedNickname);
            return true;
        }

        // Check auto-login
        try {
            const autoLogin = await api.autoLogin();
            if (autoLogin === true) {
                const data = await api.login({username: '', verified: ''});
                if (data && data.token) {
                    state.token = data.token;
                    state.username = data.nickname || '用户';
                    localStorage.setItem(this.STORAGE_KEY_TOKEN, data.token);
                    localStorage.setItem(this.STORAGE_KEY_NICKNAME, data.nickname || '用户');
                    this.renderUserState(data.nickname || '用户');
                    return true;
                }
            } else {
                this.renderLoggedOut();
            }
        } catch (e) {
            this.renderLoggedOut();
        }
        return false;
    },

    /** Render the user state in the top-right corner */
    renderUserState(nickname) {
        let el = document.getElementById('user-info-display');
        if (!el) {
            el = document.createElement('div');
            el.id = 'user-info-display';
            const headerActions = document.querySelector('.header-actions');
            if (headerActions) {
                headerActions.prepend(el);
            }
        }
        el.textContent = nickname;
        el.className = 'user-info-display';
        el.onclick = () => this.showLoggedOutMessage();
    },

    /** Show "未登录" in the top-right corner */
    renderLoggedOut() {
        let el = document.getElementById('user-info-display');
        if (!el) {
            el = document.createElement('div');
            el.id = 'user-info-display';
            const headerActions = document.querySelector('.header-actions');
            if (headerActions) {
                headerActions.prepend(el);
            }
        }
        el.textContent = '未登录';
        el.className = 'user-info-display user-logged-out';
        el.onclick = () => this.showLoggedOutMessage();
    },

    /** Show login modal when clicking "未登录" */
    showLoggedOutMessage() {
        if (state.token) return; // already logged in
        let modal = document.getElementById('login-modal');
        if (!modal) {
            modal = document.createElement('div');
            modal.id = 'login-modal';
            modal.className = 'modal-overlay';
            modal.innerHTML = `
                <div class="modal-content" style="max-width: 400px;">
                    <div class="modal-header">
                        <h3>登录</h3>
                        <div class="close-button" id="login-modal-close">&times;</div>
                    </div>
                    <div class="modal-body">
                        <div style="margin-bottom: 16px;">
                            <label style="display: block; margin-bottom: 6px; font-size: 14px; color: var(--text-secondary);">用户名</label>
                            <input type="text" id="login-username-input" placeholder="请输入用户名" style="width: 100%; padding: 10px 12px; border: 1px solid var(--border-color); border-radius: 6px; font-size: 14px; box-sizing: border-box;" />
                        </div>
                        <div style="margin-bottom: 24px;">
                            <label style="display: block; margin-bottom: 6px; font-size: 14px; color: var(--text-secondary);">验证码</label>
                            <input type="password" id="login-verified-input" placeholder="请输入验证码" style="width: 100%; padding: 10px 12px; border: 1px solid var(--border-color); border-radius: 6px; font-size: 14px; box-sizing: border-box;" />
                        </div>
                        <button id="login-submit-btn" style="width: 100%; padding: 10px; background: var(--primary-color); color: #fff; border: none; border-radius: 6px; font-size: 14px; cursor: pointer;">登录</button>
                    </div>
                </div>
            `;
            modal.addEventListener('click', (e) => {
                if (e.target === modal) modal.style.display = 'none';
            });
            document.body.appendChild(modal);

            // Bind events
            modal.querySelector('#login-modal-close').addEventListener('click', () => {
                modal.style.display = 'none';
            });
            modal.querySelector('#login-submit-btn').addEventListener('click', async () => {
                const username = modal.querySelector('#login-username-input').value.trim();
                const verified = modal.querySelector('#login-verified-input').value.trim();
                if (!username) {
                    showToast('请输入用户名', 'error');
                    return;
                }
                const data = await api.login({username, verified});
                if (data && data.token) {
                    state.token = data.token;
                    state.username = data.nickname || username;
                    localStorage.setItem(this.STORAGE_KEY_TOKEN, data.token);
                    localStorage.setItem(this.STORAGE_KEY_NICKNAME, data.nickname || username);
                    this.renderUserState(data.nickname || username);
                    modal.style.display = 'none';
                    showToast('登录成功', 'success');
                    // Reload data after login
                    await mcp.loadList();
                    await skills.loadList();
                    await conversation.loadList();
                } else {
                    showToast('登录失败，请检查用户名和验证码', 'error');
                }
            });

            // Enter key to submit
            modal.querySelector('#login-verified-input').addEventListener('keydown', (e) => {
                if (e.key === 'Enter') {
                    modal.querySelector('#login-submit-btn').click();
                }
            });
        }
        modal.style.display = 'flex';
        // Focus username input
        setTimeout(() => modal.querySelector('#login-username-input')?.focus(), 100);
    },

    /** Clear auth state — called on 401 or logout */
    clear() {
        state.token = null;
        state.username = null;
        localStorage.removeItem(this.STORAGE_KEY_TOKEN);
        localStorage.removeItem(this.STORAGE_KEY_NICKNAME);
        this.renderLoggedOut();
    },

    async login(username, password) {
        const data = await api.login({ username, password });
        if (data) {
            state.username = data.username;
            state.token = data.verified || data.token || data.username;
            return true;
        }
        return false;
    },
};

// ===================== §12 UI Components (only DOM manipulation) =====================
const aiImage = '/static/ai.jpg';
const userImage = '/static/user.png';

const ui = {
    mainContent: null,

    init() {
        this.mainContent = document.getElementById('mainContent');
    },

    clearChat() {
        this.mainContent.innerHTML = `
            <div class="welcome-message">
                <h2>你好！我是你的 AI 助手</h2>
                <p>有什么我可以帮助你的吗？现在可以开始聊天了</p>
            </div>`;
    },

    renderUserMessage(text) {
        const item = document.createElement('div');
        item.className = 'chat-item chat-item-right';
        item.innerHTML = `
            <div class="bubble"><div style="margin: 16px">${renderMarkdown(text)}</div></div>
            <div class="avatar"><img src="${userImage}" alt="用户"/></div>`;
        this.mainContent.appendChild(item);
        this.scrollToBottom();
    },

    renderBotMessage(id) {
        const item = document.createElement('div');
        item.className = 'chat-item chat-item-left';
        item.innerHTML = `
            <div class="avatar"><img src="${aiImage}" alt="AI"/></div>
            <div class="bubble">
                <div class="thinking-container" id="thinking-${id}" style="display: none;">
                    <div class="thinking-header" onclick="ui.toggleThinking('${id}')">
                        <span class="thinking-title">思考过程</span>
                        <span class="thinking-arrow" id="arrow-${id}">▼</span>
                    </div>
                    <div class="thinking-content" id="thinking-content-${id}">
                        <div class="thinking-body" id="thinking-body-${id}"></div>
                    </div>
                </div>
                <div id="origin-${id}" style="display: none"></div>
                <div id="${id}" style="margin: 16px"></div>
                <div class="bubble-actions" id="actions-${id}" style="display: none;">
                    <button class="bubble-action-btn" onclick="ui.copyMarkdown('origin-${id}')">
                        <span>📋</span><span>复制</span>
                    </button>
                    <button class="bubble-action-btn" onclick="ui.downloadMarkdown('origin-${id}')">
                        <span>💾</span><span>下载</span>
                    </button>
                </div>
            </div>`;
        this.mainContent.appendChild(item);
        this.scrollToBottom();
    },

    renderMessages(messages) {
        this.clearChat();
        if (!messages || messages.length === 0) return;
        for (const msg of messages) {
            const role = msg.messageType || msg.role || msg.getMessage?.();
            const content = msg.text || msg.content || msg.getContent?.() || msg.getText?.() || '';
            if (role === 'USER' || role === 'user') {
                this.renderUserMessage(content);
            } else if (role === 'ASSISTANT' || role === 'assistant' || role === 'MODEL') {
                const id = 'hist-' + Date.now() + '-' + Math.random().toString(36).slice(2, 6);
                this.renderBotMessage(id);
                const el = document.getElementById(id);
                if (el) el.innerHTML = renderMarkdown(content);
                const origin = document.getElementById('origin-' + id);
                if (origin) origin.innerHTML = content;
                // Show actions for historical messages
                const actions = document.getElementById('actions-' + id);
                if (actions) actions.style.display = '';
            }
        }
    },

    scrollToBottom() {
        if (this.mainContent) this.mainContent.scrollTop = this.mainContent.scrollHeight;
    },

    toggleThinking(id) {
        const content = document.getElementById(`thinking-content-${id}`);
        const arrow = document.getElementById(`arrow-${id}`);
        if (!content || !arrow) return;
        content.classList.toggle('expanded');
        arrow.classList.toggle('expanded');
    },

    copyMarkdown(id) {
        const el = document.getElementById(id);
        if (!el) { showToast('消息未找到', 'error'); return; }
        const text = el.textContent;
        if (!text || !text.trim()) { showToast('没有可复制的内容', 'error'); return; }
        navigator.clipboard.writeText(text)
            .then(() => showToast('复制成功！', 'success'))
            .catch(() => showToast('复制失败，请手动复制', 'error'));
    },

    downloadMarkdown(id) {
        const el = document.getElementById(id);
        if (!el) { showToast('消息未找到', 'error'); return; }
        const content = el.textContent;
        if (!content || !content.trim()) { showToast('没有可下载的内容', 'error'); return; }
        const blob = new Blob([content], { type: 'text/markdown;charset=utf-8' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        const ts = new Date().toISOString().replace(/[:.]/g, '-').slice(0, -5);
        link.download = `chat-${ts}.md`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(url);
        showToast('下载成功！', 'success');
    },

    enableSend() {
        const ta = document.getElementById('textarea');
        const btn = document.getElementById('send-btn');
        ta.disabled = false;
        btn.disabled = false;
        btn.textContent = '发送消息';
        state.isStreaming = false;
    },

    disableSend() {
        const ta = document.getElementById('textarea');
        const btn = document.getElementById('send-btn');
        ta.value = '';
        ta.disabled = true;
        btn.disabled = true;
        btn.textContent = '发送中...';
        state.isStreaming = true;
    },

    showModal(id) {
        document.getElementById(id).style.display = 'flex';
    },

    hideModal(id) {
        document.getElementById(id).style.display = 'none';
    },

    toggleSidebar() {
        const sidebar = document.getElementById('sidebar');
        sidebar.classList.toggle('open');
    },
};

// ===================== §6 Conversation Management =====================
const conversation = {
    async loadList() {
        const data = await api.listConversations();
        this.renderSidebar(data);
    },

    renderSidebar(list) {
        const container = document.getElementById('sidebarList');
        if (!list || list.length === 0) {
            container.innerHTML = '<div class="sidebar-empty">暂无对话</div>';
            return;
        }
        container.innerHTML = '';
        for (const item of list) {
            const id = item.conversationId || item.id || item.id;
            const title = item.title || truncateText(item.name || '新对话', API.titleMaxLength);
            const div = document.createElement('div');
            div.className = 'sidebar-item' + (state.conversationId === id ? ' active' : '');
            div.innerHTML = `
                <span class="sidebar-item-text" title="${title}">${title}</span>
                <button class="sidebar-item-delete" title="删除对话">&times;</button>`;
            // Listen on parent .sidebar-item so it works in both full-width and collapsed (icon-only) modes
            div.addEventListener('click', (e) => {
                if (e.target.classList.contains('sidebar-item-delete')) return;
                this.switchTo(id);
            });
            div.querySelector('.sidebar-item-delete').addEventListener('click', (e) => {
                e.stopPropagation();
                this.delete(id);
            });
            container.appendChild(div);
        }
    },

    createNew() {
        state.conversationId = crypto.randomUUID();
        ui.clearChat();
        imageUpload.clear();
        // refresh sidebar highlight
        this.loadList();
    },

    async switchTo(id) {
        // abort any ongoing stream
        chat.abortStream();

        state.conversationId = id;
        try {
            const messages = await api.getConversationMessages(id);
            ui.renderMessages(messages);
        } catch (e) {
            showToast('加载对话失败', 'error');
        }
        this.loadList(); // re-render highlight
    },

    async delete(id) {
        if (!confirm('确定要删除这个对话吗？')) return;
        const ok = await api.deleteConversation(id);
        if (ok) {
            if (state.conversationId === id) {
                this.createNew();
            }
            this.loadList();
            showToast('对话已删除', 'success');
        } else {
            showToast('删除失败', 'error');
        }
    },

    refreshSidebar() {
        this.loadList();
    },
};

// ===================== §7 Chat Engine =====================
const chat = {
    async send() {
        const ta = document.getElementById('textarea');
        const text = ta.value.trim();
        if (!text && !state.isStreaming) {
            ui.renderUserMessage('');
            const el = document.getElementById('msg-empty');
            showToast('请输入消息内容', 'error');
            return;
        }

        ui.renderUserMessage(text);
        ui.disableSend();

        const id = Date.now();
        ui.renderBotMessage(id);

        const answerEl = document.getElementById(id);
        const originEl = document.getElementById('origin-' + id);
        const thinkingEl = document.getElementById('thinking-body-' + id);

        let answerText = '';
        let reasonText = '';

        const record = {
            message: text,
            conversationId: state.conversationId,
            mcps: state.selectedMcps,
            enableRag: state.enableRag,
            knowledgeId: state.selectedKnowledgeId || null,
            authentication: state.token || '',
            fileId: state.pendingImage?.fileId || null,
        };

        // Clear pending image after capturing fileId
        imageUpload.clear();

        try {
            await api.streamChat(record,
                (data) => {
                    // reasoning content
                    if (data.reasoningContent) {
                        const thinkingContainer = document.getElementById('thinking-' + id);
                        if (thinkingContainer) thinkingContainer.style.display = '';
                        reasonText += data.reasoningContent;
                        if (thinkingEl) thinkingEl.innerHTML = renderMarkdown(reasonText);
                    }
                    // answer content
                    if (data.content) {
                        answerText += data.content;
                        if (answerEl) answerEl.innerHTML = renderMarkdown(answerText);
                        if (originEl) originEl.innerHTML = answerText;
                    }
                    ui.scrollToBottom();
                },
                () => {
                    // complete
                    const actionsEl = document.getElementById('actions-' + id);
                    if (actionsEl) actionsEl.style.display = '';
                    ui.enableSend();
                    conversation.loadList();
                },
                (error) => {
                    // error
                    const actionsEl = document.getElementById('actions-' + id);
                    if (actionsEl) actionsEl.style.display = '';
                    if (answerEl) answerEl.innerHTML += '<br/><span style="color:var(--error-color)">发送失败：' + (error.message || '未知错误') + '</span>';
                    ui.enableSend();
                }
            );
        } catch (error) {
            if (answerEl) answerEl.innerHTML += '<br/><span style="color:var(--error-color)">发送失败：' + error.message + '</span>';
            ui.enableSend();
        }
    },

    abortStream() {
        if (state.isStreaming) {
            ui.enableSend();
        }
    },
};

// ===================== §8 Knowledge Space =====================
const knowledge = {
    currentKbId: null,

    openPanel() {
        ui.showModal('ks-modal-overlay');
        this.loadList();
    },

    closePanel() {
        ui.hideModal('ks-modal-overlay');
    },

    async loadList() {
        const data = await api.listKnowledge();
        this.renderList(data);
    },

    renderList(list) {
        const container = document.getElementById('ks-sidebar');
        if (!list || list.length === 0) {
            container.innerHTML = '<div class="sidebar-empty" style="padding: 40px 16px;">暂无知识库</div>';
            return;
        }
        container.innerHTML = '';

        // "No knowledge base" option at top
        const noneDiv = document.createElement('div');
        noneDiv.className = 'ks-item' + (state.selectedKnowledgeId === null ? ' active' : '');
        noneDiv.innerHTML = `
            <input type="radio" name="ks-select" value="" ${state.selectedKnowledgeId === null ? 'checked' : ''} style="width: 16px; height: 16px; cursor: pointer; flex-shrink: 0;">
            <span class="ks-item-name">不使用知识库</span>`;
        noneDiv.querySelector('input[type="radio"]').addEventListener('change', () => this.selectKnowledgeForChat(null));
        noneDiv.querySelector('.ks-item-name').addEventListener('click', () => this.selectKnowledgeForChat(null));
        container.appendChild(noneDiv);

        for (const kb of list) {
            const id = kb.id;
            const name = kb.name;
            const div = document.createElement('div');
            div.className = 'ks-item' + (state.selectedKnowledgeId === id ? ' active' : '');
            div.innerHTML = `
                <input type="radio" name="ks-select" value="${id}" ${state.selectedKnowledgeId === id ? 'checked' : ''} style="width: 16px; height: 16px; cursor: pointer; flex-shrink: 0;">
                <span class="ks-item-name">${name}</span>
                <button class="ks-item-delete">&times;</button>`;
            div.querySelector('input[type="radio"]').addEventListener('change', () => {
                this.selectKnowledgeForChat(id);
                this.select(id, name);
            });
            div.querySelector('.ks-item-name').addEventListener('click', (e) => {
                e.stopPropagation();
                this.selectKnowledgeForChat(id);
                // Also open detail panel to show files
                this.select(id, name);
            });
            div.querySelector('.ks-item-delete').addEventListener('click', (e) => {
                e.stopPropagation();
                this.delete(id);
            });
            container.appendChild(div);
        }
    },

    /** Select a knowledge base for chat (single-select / radio behavior) */
    selectKnowledgeForChat(id) {
        state.selectedKnowledgeId = id;
        // Update active class on sidebar items directly (no re-render to preserve event listeners)
        const items = document.querySelectorAll('#ks-sidebar .ks-item');
        items.forEach(item => {
            const radio = item.querySelector('input[type="radio"]');
            if (radio) {
                const itemId = radio.value === '' ? null : radio.value;
                const isActive = itemId === id;
                item.classList.toggle('active', isActive);
                radio.checked = isActive;
            }
        });
        // If selecting "no knowledge base", clear the detail panel
        if (id === null) {
            const detail = document.getElementById('ks-detail');
            detail.innerHTML = '<div style="padding: 40px; text-align: center; color: var(--text-muted);">选择一个知识库查看文件</div>';
        }
    },

    async create() {
        const name = prompt('请输入知识库名称：');
        if (!name || !name.trim()) return;
        const data = await api.createKnowledge(name.trim());
        if (data) {
            showToast('知识库创建成功', 'success');
            this.loadList();
        } else {
            showToast('创建失败', 'error');
        }
    },

    async delete(id) {
        if (!confirm('确定要删除这个知识库吗？关联文件将被一并移除。')) return;
        const ok = await api.deleteKnowledge(id);
        if (ok) {
            if (this.currentKbId === id) {
                this.currentKbId = null;
                document.getElementById('ks-detail').innerHTML = '<div style="padding: 40px; text-align: center; color: var(--text-muted);">选择一个知识库查看文件</div>';
            }
            this.loadList();
            showToast('知识库已删除', 'success');
        } else {
            showToast('删除失败', 'error');
        }
    },

    async select(id, name) {
        this.currentKbId = id;

        // show detail
        const detail = document.getElementById('ks-detail');
        detail.innerHTML = `
            <div class="ks-detail-header">
                <span class="ks-detail-title">${name}</span>
                <div>
                    <button class="ks-upload-btn" id="ks-upload-btn">+ 上传文件</button>
                    <input type="file" id="ks-file-input" style="display:none;">
                </div>
            </div>
            <div class="ks-file-list"><div class="loading-indicator">加载中...</div></div>`;

        const uploadBtn = detail.querySelector('#ks-upload-btn');
        const fileInput = detail.querySelector('#ks-file-input');
        uploadBtn.addEventListener('click', () => fileInput.click());
        fileInput.addEventListener('change', (e) => this.uploadFile(id, e));

        this.loadFiles(id);
    },

    async loadFiles(kbId) {
        const container = document.getElementById('ks-detail').querySelector('.ks-file-list');
        try {
            const files = await api.listKnowledgeFiles(kbId);
            if (!files || files.length === 0) {
                container.innerHTML = '<div style="padding: 40px; text-align: center; color: var(--text-muted);">暂无文件</div>';
                return;
            }
            container.innerHTML = `
                <table class="knowledge-table">
                    <thead><tr><th>文件名</th><th>大小</th><th>上传时间</th><th>操作</th></tr></thead>
                    <tbody id="ks-file-tbody"></tbody>
                </table>`;
            const tbody = document.getElementById('ks-file-tbody');
            for (const f of files) {
                const row = document.createElement('tr');
                row.innerHTML = `
                    <td>${truncateText(f.fileName || f.name || '', 30)}</td>
                    <td>${formatFileSize(f.size || 0)}</td>
                    <td>${formatDate(f.uploadTime || f.createTime)}</td>
                    <td><button class="action-btn" data-file-id="${f.id}">删除</button></td>`;
                row.querySelector('.action-btn').addEventListener('click', () => this.deleteFile(kbId, f.id, row));
                tbody.appendChild(row);
            }
        } catch (e) {
            container.innerHTML = '<div style="padding: 40px; text-align: center; color: var(--error-color);">加载失败</div>';
        }
    },

    async uploadFile(kbId, event) {
        const file = event.target.files[0];
        if (!file) return;
        try {
            const data = await api.uploadToKnowledge(kbId, file);
            if (data) {
                showToast(`文件 "${file.name}" 上传成功`, 'success');
                this.loadFiles(kbId);
            }
        } catch (e) {
            showToast('上传失败', 'error');
        }
        event.target.value = '';
    },

    async deleteFile(kbId, fileId, row) {
        if (!confirm('确定要删除这个文件吗？')) return;
        try {
            const ok = await api.deleteKnowledgeFile(kbId, fileId);
            if (ok) {
                row.remove();
                showToast('文件已删除', 'success');
            }
        } catch (e) {
            showToast('删除失败', 'error');
        }
    },

};

// ===================== §9 MCP Service =====================
const mcp = {
    openModal() {
        ui.showModal('mcp-modal-overlay');
        this.renderModal();
    },

    closeModal() {
        ui.hideModal('mcp-modal-overlay');
    },

    renderModal() {
        const container = document.getElementById('mcp-list');
        const detail = document.getElementById('mcp-detail');
        detail.innerHTML = '<div style="padding: 40px; text-align: center; color: var(--text-muted);"><p style="font-size: 16px; margin-bottom: 8px;">请选择一个MCP服务查看详情</p></div>';

        if (state.mcps.length === 0) {
            container.innerHTML = '<div style="padding: 20px; text-align: center; color: var(--text-muted);">暂无可用MCP服务</div>';
            return;
        }
        container.innerHTML = '';
        for (const m of state.mcps) {
            const item = document.createElement('div');
            item.className = 'skill-item' + (state.selectedMcps.includes(m.name) ? ' selected' : '');
            item.innerHTML = `
                <div style="display: flex; align-items: center; gap: 12px;">
                    <input type="checkbox" ${state.selectedMcps.includes(m.name) ? 'checked' : ''} style="width: 18px; height: 18px; cursor: pointer;" class="mcp-checkbox">
                    <div class="mcp-item-text" style="flex: 1; cursor: pointer;">
                        <div class="skill-item-name">${m.title || m.name}</div>
                    </div>
                </div>`;
            item.querySelector('.mcp-checkbox').addEventListener('click', (e) => {
                e.stopPropagation();
                this.toggleSelect(m.name, item);
            });
            item.querySelector('.mcp-item-text').addEventListener('click', () => this.showDetail(m));
            container.appendChild(item);
        }
    },

    toggleSelect(name, element) {
        const idx = state.selectedMcps.indexOf(name);
        if (idx >= 0) {
            state.selectedMcps.splice(idx, 1);
            element.classList.remove('selected');
        } else {
            state.selectedMcps.push(name);
            element.classList.add('selected');
        }
        showToast(`已${state.selectedMcps.includes(name) ? '选中' : '取消'}MCP服务`, 'success');
    },

    showDetail(m) {
        document.getElementById('mcp-detail-title').textContent = m.title || m.name;
        const detail = document.getElementById('mcp-detail');
        let html = '';

        // Basic info
        html += `<div class="detail-section">
            <div class="detail-section-title">基本信息</div>
            <div style="line-height: 1.8; color: var(--text-primary);">
                <div style="margin-bottom: 12px;"><strong>名称：</strong>${m.name}</div>
                <div style="margin-bottom: 12px;"><strong>版本：</strong>${m.version || '1.0.0'}</div>
                <div><strong>描述：</strong>${m.description || '无描述'}</div>
            </div>
        </div>`;

        // Tools
        const tools = m.tools || [];
        html += `<div class="detail-section">
            <div class="detail-section-title">包含工具 (${tools.length})</div>
            <div style="display: flex; flex-direction: column; gap: 12px;">`;
        if (tools.length > 0) {
            for (const tool of tools) {
                html += `<div style="padding: 16px; background: var(--bg-primary); border: 1px solid var(--border-color); border-radius: 8px;">
                    <div style="font-weight: 600; font-size: 14px; color: var(--primary-color); margin-bottom: 8px;">${tool.title || tool.name}</div>
                    <div style="font-size: 13px; color: var(--text-secondary); line-height: 1.6; white-space: pre-wrap;">${tool.description || '无描述'}</div>
                </div>`;
            }
        } else {
            html += '<span style="color: var(--text-muted); font-size: 13px;">无可用工具</span>';
        }
        html += '</div></div>';
        detail.innerHTML = html;
    },

    async loadList() {
        const data = await api.listMcps();
        if (data && data.length > 0) {
            state.mcps = data;
            state.selectedMcps = data.filter(m => m.defaultSelected).map(m => m.name);
            document.getElementById('mcp-button').style.display = 'flex';
        } else {
            state.mcps = [];
            state.selectedMcps = [];
            document.getElementById('mcp-button').style.display = 'none';
        }
    },
};

// ===================== §10 Skills =====================
const skills = {
    allTools: [],

    openModal() {
        ui.showModal('skills-modal-overlay');
        this.renderModal();
    },

    closeModal() {
        ui.hideModal('skills-modal-overlay');
    },

    renderModal() {
        const container = document.getElementById('skills-list');
        const detail = document.getElementById('skills-detail');
        detail.innerHTML = '<div style="padding: 40px; text-align: center; color: var(--text-muted);"><p style="font-size: 16px; margin-bottom: 8px;">请选择一个技能查看详情</p></div>';
        container.innerHTML = '<div style="padding: 20px; text-align: center; color: var(--text-muted);">加载中...</div>';

        api.listSkills().then(data => {
            container.innerHTML = '';
            if (!data || data.length === 0) {
                container.innerHTML = '<div style="padding: 20px; text-align: center; color: var(--text-muted);">暂无可用技能</div>';
                return;
            }
            // collect all tools
            skills.allTools = [];
            if (data.length > 0 && data[0] && data[0].tools) {
                // get tools from skills
            }
            for (const skill of data) {
                if (skill.tools) skills.allTools.push(...skill.tools);
            }

            for (const skill of data) {
                const item = document.createElement('div');
                item.className = 'skill-item';
                item.innerHTML = `<div class="skill-item-name">${skill.name}</div>`;
                item.addEventListener('click', () => this.select(skill, item));
                container.appendChild(item);
            }
        }).catch(() => {
            container.innerHTML = '<div style="padding: 40px; text-align: center; color: var(--error-color);">加载失败</div>';
        });
    },

    select(skill, element) {
        state.selectedSkill = skill;
        const allItems = document.querySelectorAll('#skills-list .skill-item');
        allItems.forEach(i => i.classList.remove('selected'));
        element.classList.add('selected');

        document.getElementById('skill-detail-title').textContent = skill.name;
        const detail = document.getElementById('skills-detail');
        let html = '';

        // Tools
        html += `<div class="detail-section">
            <div class="detail-section-title">使用工具</div>
            <div class="tools-tags" id="tools-tags"></div>
        </div>`;

        // Content
        html += `<div class="detail-section">
            <div class="detail-section-title">技能说明</div>
            <div class="detail-section-content" style="line-height: 1.8; color: var(--text-primary);">
                ${skill.content?.text ? renderMarkdown(skill.content.text) : '<span style="color: var(--text-muted);">无详细说明</span>'}
            </div>
        </div>`;

        // Parameters
        html += `<div class="detail-section">
            <div class="detail-section-title">参数设置</div>
            <div id="params-container" style="display: flex; flex-direction: column; gap: 16px;"></div>
        </div>`;

        // Send button
        html += `<div style="margin-top: 24px;">
            <button class="send-skill-btn" id="send-skill-btn">应用技能并发送</button>
        </div>`;

        detail.innerHTML = html;

        // Render tools tags
        const tagsContainer = detail.querySelector('#tools-tags');
        if (skill.tools && skill.tools.length > 0) {
            for (const toolName of skill.tools) {
                const tag = document.createElement('span');
                tag.className = 'tool-tag';
                tag.textContent = toolName;
                tagsContainer.appendChild(tag);
            }
        } else {
            tagsContainer.innerHTML = '<span style="color: var(--text-muted); font-size: 13px;">无指定工具</span>';
        }

        // Render params
        const paramsContainer = detail.querySelector('#params-container');
        const skillParams = {};
        if (skill.params && skill.params.length > 0) {
            for (const param of skill.params) {
                const group = document.createElement('div');
                group.className = 'param-input-group';

                const requiredMark = param.required ? '<span style="color: var(--error-color); margin-left: 4px;">*</span>' : '';
                let inputHtml = '';

                if (param.type === 'SELECT') {
                    inputHtml = `<select class="param-input" data-param="${param.name}" data-required="${param.required}">`;
                    if (!param.required) inputHtml += '<option value="">请选择</option>';
                    if (param.options && param.options.length > 0) {
                        for (const opt of param.options) {
                            const sel = param.defaultValue === opt.value ? 'selected' : '';
                            inputHtml += `<option value="${opt.value}" ${sel}>${opt.label}</option>`;
                        }
                    }
                    inputHtml += '</select>';
                } else if (param.type === 'TEXT_AREA') {
                    inputHtml = `<textarea class="param-input param-textarea" placeholder="${param.placeholder || '请输入' + param.label}" data-param="${param.name}" data-required="${param.required}">${param.defaultValue || ''}</textarea>`;
                } else {
                    inputHtml = `<input type="text" class="param-input" placeholder="${param.placeholder || '请输入' + param.label}" value="${param.defaultValue || ''}" data-param="${param.name}" data-required="${param.required}">`;
                }

                group.innerHTML = `<label class="param-label">${param.label}${requiredMark}</label>` + inputHtml;

                const input = group.querySelector('.param-input');
                input.addEventListener('input', (e) => { skillParams[param.name] = e.target.value; });
                input.addEventListener('change', (e) => { skillParams[param.name] = e.target.value; });
                if (param.defaultValue) skillParams[param.name] = param.defaultValue;

                paramsContainer.appendChild(group);
            }
        } else {
            paramsContainer.innerHTML = '<span style="color: var(--text-muted); font-size: 13px;">无需设置参数</span>';
        }

        // Send skill button handler
        detail.querySelector('#send-skill-btn').addEventListener('click', () => {
            this.send(skill, skillParams);
        });
    },

    send(skill, skillParams) {
        // Validate required params
        if (skill.params && skill.params.length > 0) {
            const missing = [];
            for (const param of skill.params) {
                if (param.required && (!skillParams[param.name] || skillParams[param.name].trim() === '')) {
                    missing.push(param.label || param.name);
                }
            }
            if (missing.length > 0) {
                showToast(`请填写完整参数：${missing.join(', ')}`, 'error');
                return;
            }
        }

        // Build prompt
        let promptContent = skill.content.text;
        if (skill.params && skill.params.length > 0) {
            for (const param of skill.params) {
                const regex = new RegExp(`\\{${param.name}\\}`, 'g');
                promptContent = promptContent.replace(regex, skillParams[param.name] || '');
            }
        }

        this.closeModal();
        document.getElementById('textarea').value = promptContent;

        // Auto-select associated MCPs
        if (skill.tools) {
            for (const toolName of skill.tools) {
                if (!state.selectedMcps.includes(toolName)) {
                    state.selectedMcps.push(toolName);
                }
            }
        }

        chat.send();
    },

    async loadList() {
        const data = await api.listSkills();
        if (data && data.length > 0) {
            document.getElementById('skills-button').style.display = 'flex';
        } else {
            document.getElementById('skills-button').style.display = 'none';
        }
    },
};

// ===================== §11 Responsive =====================
const responsive = {
    handleResize() {
        const sidebar = document.getElementById('sidebar');
        const toggle = document.getElementById('sidebar-toggle');
        if (window.innerWidth < 768) {
            toggle.style.display = 'flex';
            sidebar.classList.remove('open');
        } else if (window.innerWidth < 1200) {
            toggle.style.display = 'none';
            sidebar.classList.remove('open');
        } else {
            toggle.style.display = 'none';
        }
    },
};

// ===================== §15 Image Upload Module =====================
const imageUpload = {
    ALLOWED_TYPES: ['image/jpeg', 'image/png', 'image/gif', 'image/webp', 'image/bmp'],
    MAX_SIZE: 10 * 1024 * 1024, // 10MB
    _objectUrl: null, // track current object URL for cleanup

    init() {
        const addBtn = document.getElementById('image-add-btn');
        const fileInput = document.getElementById('image-file-input');

        addBtn.addEventListener('click', () => fileInput.click());
        fileInput.addEventListener('change', (e) => this.handleFile(e));
        document.getElementById('thumbnail-remove').addEventListener('click', (e) => {
            e.stopPropagation();
            this.remove();
        });
        document.getElementById('image-thumbnail').addEventListener('dblclick', () => {
            if (state.pendingImage) {
                this.showFullscreen(state.pendingImage.objectUrl);
            }
        });
    },

    validate(file) {
        if (!this.ALLOWED_TYPES.includes(file.type)) {
            return { valid: false, error: '不支持的图片格式，请选择 JPG/PNG/GIF/WebP/BMP 格式' };
        }
        if (file.size > this.MAX_SIZE) {
            return { valid: false, error: '图片大小不能超过 10MB' };
        }
        return { valid: true };
    },

    async handleFile(event) {
        const file = event.target.files?.[0];
        if (!file) return;

        // Reset file input so same file can be selected again
        event.target.value = '';

        const validation = this.validate(file);
        if (!validation.valid) {
            showToast(validation.error, 'error');
            return;
        }

        // Show instant thumbnail with loading state
        const objectUrl = URL.createObjectURL(file);
        this.renderThumbnail(null, objectUrl, file.name);

        // Upload to server
        try {
            const { fileId } = await api.uploadImage(file);

            // Revoke old object URL
            if (this._objectUrl) {
                URL.revokeObjectURL(this._objectUrl);
            }

            // Save to state
            state.pendingImage = { fileId, objectUrl, fileName: file.name };
            this._objectUrl = objectUrl;

            // Update thumbnail (remove loading state)
            this.renderThumbnail(fileId, objectUrl, file.name);
        } catch (err) {
            URL.revokeObjectURL(objectUrl);
            this.remove();
            showToast('图片上传失败：' + err.message, 'error');
        }
    },

    renderThumbnail(fileId, objectUrl, fileName) {
        const uploadArea = document.getElementById('image-upload-area');
        const thumbnailImg = document.getElementById('thumbnail-img');
        const loadingEl = document.getElementById('thumbnail-loading');

        if (!objectUrl) {
            uploadArea.style.display = 'none';
            return;
        }

        uploadArea.style.display = 'block';
        thumbnailImg.src = objectUrl;
        thumbnailImg.alt = fileName;

        if (fileId) {
            loadingEl.style.display = 'none';
        } else {
            loadingEl.style.display = 'flex';
        }
    },

    remove() {
        if (state.pendingImage?.objectUrl) {
            URL.revokeObjectURL(state.pendingImage.objectUrl);
        }
        state.pendingImage = null;
        this._objectUrl = null;
        this.renderThumbnail(null, null, null);
    },

    clear() {
        this.remove();
    },

    showFullscreen(objectUrl) {
        const overlay = document.getElementById('image-viewer-overlay');
        const image = document.getElementById('viewer-image');
        image.src = objectUrl;
        overlay.style.display = 'flex';
    },

    hideFullscreen() {
        const overlay = document.getElementById('image-viewer-overlay');
        const image = document.getElementById('viewer-image');
        overlay.style.display = 'none';
        image.src = '';
    },
};

// ===================== §14 Initialization =====================
const init = async () => {
    ui.init();

    // Auth check — auto-login or restore from localStorage
    const loggedIn = await auth.init();

    // Only load protected resources after successful login
    if (loggedIn) {
        // Load MCPs
        await mcp.loadList();

        // Load Skills
        await skills.loadList();
    }

    // Feature detection (image upload)
    try {
        const uploadOk = await api.checkKnowledgeUpload();
        if (uploadOk) {
            document.getElementById('image-add-btn').style.display = 'flex';
        }
    } catch { /* upload not available */
    }

    // Load conversations
    await conversation.loadList();

    // Create initial conversation if none
    if (!state.conversationId) {
        conversation.createNew();
    }

    // Event bindings
    const textarea = document.getElementById('textarea');
    const sendBtn = document.getElementById('send-btn');

    textarea.addEventListener('keydown', (event) => {
        if (event.key === 'Enter' && event.ctrlKey) {
            event.preventDefault();
            const start = textarea.selectionStart;
            const end = textarea.selectionEnd;
            textarea.value = textarea.value.substring(0, start) + '\n' + textarea.value.substring(end);
            textarea.setSelectionRange(start + 1, start + 1);
        }
        if (event.key === 'Enter' && !event.shiftKey && !event.ctrlKey) {
            event.preventDefault();
            chat.send();
        }
    });

    sendBtn.addEventListener('click', () => chat.send());

    // Modal overlay click-to-close
    document.getElementById('mcp-modal-overlay').addEventListener('click', (e) => {
        if (e.target === e.currentTarget) mcp.closeModal();
    });
    document.getElementById('skills-modal-overlay').addEventListener('click', (e) => {
        if (e.target === e.currentTarget) skills.closeModal();
    });
    document.getElementById('ks-modal-overlay').addEventListener('click', (e) => {
        if (e.target === e.currentTarget) knowledge.closePanel();
    });

    // Image upload
    imageUpload.init();

    // Image viewer close handlers
    document.getElementById('image-viewer-overlay').addEventListener('click', (e) => {
        if (e.target === e.currentTarget) imageUpload.hideFullscreen();
    });
    document.getElementById('viewer-close').addEventListener('click', () => imageUpload.hideFullscreen());
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') imageUpload.hideFullscreen();
    });

    // Responsive
    responsive.handleResize();
    window.addEventListener('resize', responsive.handleResize);

    // Event listeners (replaces inline onclick handlers from ES module scope)
    const el = (sel) => document.querySelector(sel);
    const addIf = (sel, handler) => {
        const e = document.querySelector(sel);
        if (e) e.addEventListener('click', handler);
    };
    addIf('#new-chat-btn', () => conversation.createNew());
    addIf('#sidebar-toggle', () => ui.toggleSidebar());
    addIf('#ks-button', () => knowledge.openPanel());
    addIf('#mcp-button', () => mcp.openModal());
    addIf('#skills-button', () => skills.openModal());
    addIf('#mcp-close-btn', () => mcp.closeModal());
    addIf('#skills-close-btn', () => skills.closeModal());
    addIf('.ks-create-btn', () => knowledge.create());
    addIf('#ks-modal-overlay .close-button', () => knowledge.closePanel());
};

document.addEventListener('DOMContentLoaded', init);

// Expose to global for testing/debugging
window._loomAgent = {state, api, imageUpload, auth, chat, conversation};
window.ui = ui;
