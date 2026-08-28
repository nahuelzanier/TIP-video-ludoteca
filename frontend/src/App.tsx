import { useState } from 'react'

import './App.css'

function App() {
  const [count, setCount] = useState(0)

  return (
    <>
      <div>
        juego aca
      </div>
      <iframe
       src="/gameData/game1/index.html"
       title="Juego"
       className="game-frame"
      />
    </>
  )
}

export default App
