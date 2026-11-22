import * as net from 'net';

/**
 * A minimal SMTP server for the tests: it accepts every message and keeps it in `mails`. The QA install's DTS is pointed at it
 * (Deployment/Server/conf/perc/perc-email.properties: localhost:2525, no TLS, no login), so the email the form widget sends can be read back.
 */
export type Mail = { from: string; to: string[]; data: string };

export async function startSmtpSink(port = 2525): Promise<{ mails: Mail[]; close: () => Promise<void> }> {
  const mails: Mail[] = [];
  const sockets = new Set<net.Socket>();
  const server = net.createServer(socket => {
    sockets.add(socket);
    socket.on('close', () => sockets.delete(socket));
    let current: Mail = { from: '', to: [], data: '' };
    let inData = false;
    let buf = '';
    socket.write('220 qa-smtp ready\r\n');
    socket.on('data', chunk => {
      buf += chunk.toString('utf8');
      for (;;) {
        if (inData) {
          const end = buf.indexOf('\r\n.\r\n');
          if (end < 0) return;
          current.data = buf.slice(0, end);
          buf = buf.slice(end + 5);
          mails.push(current);
          current = { from: '', to: [], data: '' };
          inData = false;
          socket.write('250 OK queued\r\n');
          continue;
        }
        const nl = buf.indexOf('\r\n');
        if (nl < 0) return;
        const line = buf.slice(0, nl);
        buf = buf.slice(nl + 2);
        const cmd = line.toUpperCase();
        if (cmd.startsWith('EHLO') || cmd.startsWith('HELO')) socket.write('250-qa-smtp\r\n250 OK\r\n');
        else if (cmd.startsWith('MAIL FROM')) { current.from = line.slice(10).trim(); socket.write('250 OK\r\n'); }
        else if (cmd.startsWith('RCPT TO')) { current.to.push(line.slice(8).trim()); socket.write('250 OK\r\n'); }
        else if (cmd === 'DATA') { inData = true; socket.write('354 go ahead\r\n'); }
        else if (cmd === 'QUIT') { socket.write('221 bye\r\n'); socket.end(); return; }
        else socket.write('250 OK\r\n');   // RSET, NOOP, ...
      }
    });
    socket.on('error', () => {});
  });
  await new Promise<void>((resolve, reject) => { server.once('error', reject); server.listen(port, '127.0.0.1', () => resolve()); });
  return { mails, close: () => new Promise<void>(resolve => { server.close(() => resolve()); sockets.forEach(s => s.destroy()); }) };
}

/** Decodes quoted-printable / base64 bodies enough to search them for text. */
export function mailText(m: Mail): string {
  let t = m.data;
  t = t.replace(/=\r?\n/g, '').replace(/=([0-9A-F]{2})/g, (_, h) => String.fromCharCode(parseInt(h, 16)));
  const b64 = t.match(/Content-Transfer-Encoding: base64\r?\n\r?\n([A-Za-z0-9+/=\r\n]+)/i);
  if (b64) t += '\n' + Buffer.from(b64[1].replace(/\s+/g, ''), 'base64').toString('utf8');
  return t;
}
