export default function register({ onCommand }) {
  onCommand(async ({ body, reply }) => {
    const command = body.trim().toLowerCase()
    if (command === '/menu' || command === '.menu') {
      await reply('RexChat Menu\n\nPlugin engine siap menerima command tambahan.')
    }
  })
}
