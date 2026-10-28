(() => {
    'use strict';
    const el = id => document.getElementById(id);
    const mapa = new MapaAcompanhamento('mapaTempoReal','atualizacaoMapa','centralizarMapa');
    const token = location.pathname.split('/').filter(Boolean).pop();
    const configuracoes = {
        AGUARDANDO: ['aguardando', '◷', 'Aguardando confirmação', 'O horário combinado ainda não terminou.'],
        CONFIRMADO: ['confirmado', '✓', 'Confirmou que está bem', 'Ela encerrou este acompanhamento com uma confirmação.'],
        ATRASADO: ['atrasado', '!', 'Passou do horário sem confirmar', 'Tente entrar em contato com ela para saber se está tudo bem.'],
        CANCELADO: ['cancelado', '—', 'Acompanhamento cancelado', 'Ela encerrou sem confirmar que está bem. Se tiver dúvida, entre em contato.']
    };
    let dados = null;
    let diferencaRelogio = 0;
    let consultando = false;
    let terminado = false;
    const formatar = valor => new Date(valor).toLocaleString('pt-BR', { day:'2-digit', month:'2-digit', hour:'2-digit', minute:'2-digit' });
    function renderizar() {
        const config = configuracoes[dados.estado];
        if (!config) throw new Error('Estado de acompanhamento desconhecido.');
        el('titulo').textContent = `Acompanhando ${dados.nome}`;
        el('introducao').textContent = 'Um combinado para saber se ela está bem.';
        el('situacao').className = 'situacao ' + config[0];
        el('icone').textContent = config[1]; el('estado').textContent = config[2]; el('descricao').textContent = config[3];
        el('detalhes').hidden = false; el('prazo').textContent = formatar(dados.prazo);
        el('confirmacao').textContent = dados.encerradoEm ? `Encerrado em ${formatar(dados.encerradoEm)}.` : '';
        el('ligar').hidden = !dados.telefone;
        if (dados.telefone) el('ligar').href = 'tel:' + dados.telefone;
        mapa.conexao(true); mapa.atualizar(dados.localizacao,dados.estado,dados.agora);
        contar();
    }
    function contar() {
        if (!dados) return;
        if (!['AGUARDANDO','ATRASADO'].includes(dados.estado)) { el('contador').textContent = ''; return; }
        const segundos = Math.ceil((new Date(dados.prazo).getTime() - (Date.now() + diferencaRelogio)) / 1000);
        if (segundos <= 0) { el('contador').textContent = 'O horário combinado já passou.'; return; }
        el('contador').textContent = `Faltam ${Math.floor(segundos / 60)} min e ${String(segundos % 60).padStart(2,'0')} s`;
    }
    async function atualizar() {
        if (consultando || terminado) return;
        if (!/^[A-Za-z0-9_-]{43}$/.test(token || '')) { indisponivel('Este link está incompleto ou não é válido.'); return; }
        consultando = true; el('atualizar').disabled = true;
        const controle = new AbortController(), tempo = setTimeout(() => controle.abort(), 12000);
        try {
            const resposta = await fetch(new URL('../api/acompanhamentos/publico/' + token, location.href), { cache:'no-store', signal:controle.signal });
            const payload = await resposta.json();
            if ([404,410].includes(resposta.status)) { indisponivel(payload.mensagem || 'Este link não está mais disponível.'); return; }
            if (!resposta.ok) throw new Error('Não foi possível consultar agora.');
            dados = payload; diferencaRelogio = new Date(dados.agora).getTime() - Date.now();
            renderizar(); el('conexao').textContent = '';
            el('atualizado').textContent = 'Atualizado às ' + new Date(dados.agora).toLocaleTimeString('pt-BR');
        } catch {
            mapa.conexao(false);
            el('conexao').textContent = dados
                ? 'Sem atualização no momento. A informação acima é a última recebida; verifique sua conexão e tente novamente.'
                : 'Não foi possível carregar o acompanhamento. Verifique sua conexão e toque em Atualizar agora.';
        } finally { clearTimeout(tempo); consultando = false; el('atualizar').disabled = false; }
    }
    function indisponivel(mensagem) {
        terminado = true; dados = null; document.querySelector('.mapa-publico').hidden = true; el('titulo').textContent = 'Link indisponível';
        el('situacao').className = 'situacao indisponivel'; el('icone').textContent = '—';
        el('estado').textContent = 'Peça um novo link'; el('descricao').textContent = mensagem;
        el('detalhes').hidden = true; el('atualizar').hidden = true; el('atualizado').textContent = '';
        el('conexao').textContent = ''; el('introducao').textContent = 'Os links são temporários e exclusivos de cada acompanhamento.';
    }
    el('atualizar').addEventListener('click', atualizar);
    document.addEventListener('visibilitychange', () => { if (!document.hidden) atualizar(); });
    window.addEventListener('online', atualizar);
    setInterval(() => { if (!document.hidden) atualizar(); }, 3000);
    setInterval(contar, 1000);
    atualizar();
})();
