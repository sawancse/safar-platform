interface Props {
  avatarUrl?: string | null;
  name?: string;
  size?: number;
  className?: string;
}

function getInitials(name?: string): string {
  if (!name) return '?';
  return name.split(' ').map(w => w[0]).join('').substring(0, 2).toUpperCase();
}

function initialsColor(name?: string): string {
  const colors = ['#f97316', '#3b82f6', '#10b981', '#8b5cf6', '#ef4444', '#ec4899', '#14b8a6', '#f59e0b'];
  const hash = (name || '').split('').reduce((acc, c) => acc + c.charCodeAt(0), 0);
  return colors[hash % colors.length];
}

export default function UserAvatar({ avatarUrl, name, size = 32, className = '' }: Props) {
  const resolvedUrl = avatarUrl
    ? (avatarUrl.startsWith('http') ? avatarUrl : `${process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'}${avatarUrl}`)
    : null;

  return (
    <div
      className={`rounded-full overflow-hidden flex-shrink-0 ${className}`}
      style={{ width: size, height: size }}
    >
      {resolvedUrl ? (
        <img src={resolvedUrl} alt={name || ''} className="w-full h-full object-cover" />
      ) : (
        <div
          className="w-full h-full flex items-center justify-center text-white font-bold"
          style={{ backgroundColor: initialsColor(name), fontSize: Math.max(10, size * 0.35) }}
        >
          {getInitials(name)}
        </div>
      )}
    </div>
  );
}
