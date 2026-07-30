import { Link, Route, Routes } from 'react-router-dom';

function Home() {
  return (
    <main>
      <h1>RECONX</h1>
      <p>Trade reconciliation</p>
      <Link to="/trades">Trades</Link>
    </main>
  );
}

function Trades() {
  return (
    <main>
      <h1>Trades</h1>
      <p>Client-side route — hard refresh should still return this SPA.</p>
      <Link to="/">Home</Link>
    </main>
  );
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Home />} />
      <Route path="/trades" element={<Trades />} />
    </Routes>
  );
}
