'use client';

import { useEffect, useState, useCallback } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import { api } from '@/lib/api';
import type { NomadPost, NomadComment, NomadPostCategory } from '@/types';

const CATEGORIES: NomadPostCategory[] = ['TIP', 'MEETUP', 'SKILL_SWAP', 'RECOMMENDATION', 'QUESTION'];

const CATEGORY_STYLE: Record<string, { bg: string; text: string; icon: string }> = {
  TIP:            { bg: 'bg-green-100',  text: 'text-green-700',  icon: '💡' },
  MEETUP:         { bg: 'bg-blue-100',   text: 'text-blue-700',   icon: '🤝' },
  SKILL_SWAP:     { bg: 'bg-purple-100', text: 'text-purple-700', icon: '🔄' },
  RECOMMENDATION: { bg: 'bg-yellow-100', text: 'text-yellow-700', icon: '⭐' },
  QUESTION:       { bg: 'bg-red-100',    text: 'text-red-700',    icon: '❓' },
};

export const dynamic = 'force-dynamic';

export default function NomadPage() {
  const searchParams = useSearchParams();
  const router = useRouter();

  const city = searchParams.get('city') ?? 'Mumbai';
  const category = searchParams.get('category') ?? '';

  const [posts, setPosts] = useState<NomadPost[]>([]);
  const [loading, setLoading] = useState(true);

  // Create post form
  const [showForm, setShowForm] = useState(false);
  const [formTitle, setFormTitle] = useState('');
  const [formBody, setFormBody] = useState('');
  const [formCategory, setFormCategory] = useState<NomadPostCategory>('TIP');
  const [formCity, setFormCity] = useState(city);
  const [formLoading, setFormLoading] = useState(false);
  const [formError, setFormError] = useState('');

  // Comments
  const [expandedPost, setExpandedPost] = useState<string | null>(null);
  const [comments, setComments] = useState<Record<string, NomadComment[]>>({});
  const [commentBody, setCommentBody] = useState('');
  const [commentLoading, setCommentLoading] = useState(false);

  const token = typeof window !== 'undefined' ? localStorage.getItem('access_token') ?? '' : '';

  const fetchFeed = useCallback(async () => {
    setLoading(true);
    try {
      const result = await api.getNomadFeed(city, category || undefined);
      setPosts(result ?? []);
    } catch {
      setPosts([]);
    } finally {
      setLoading(false);
    }
  }, [city, category]);

  useEffect(() => {
    fetchFeed();
  }, [fetchFeed]);

  function updateParam(key: string, value: string) {
    const params = new URLSearchParams(searchParams.toString());
    if (value) params.set(key, value);
    else params.delete(key);
    router.push(`/nomad?${params.toString()}`);
  }

  async function handleCreatePost(e: React.FormEvent) {
    e.preventDefault();
    if (!token) {
      router.push('/auth?redirect=/nomad');
      return;
    }
    setFormLoading(true);
    setFormError('');
    try {
      await api.createNomadPost(
        { title: formTitle, body: formBody, category: formCategory, city: formCity },
        token
      );
      setFormTitle('');
      setFormBody('');
      setShowForm(false);
      fetchFeed();
    } catch (err: any) {
      setFormError(err.message || 'Failed to create post.');
    } finally {
      setFormLoading(false);
    }
  }

  async function toggleComments(postId: string) {
    if (expandedPost === postId) {
      setExpandedPost(null);
      return;
    }
    setExpandedPost(postId);
    if (!comments[postId]) {
      try {
        const cmts = await api.getNomadPostComments(postId);
        setComments((prev) => ({ ...prev, [postId]: cmts ?? [] }));
      } catch {
        setComments((prev) => ({ ...prev, [postId]: [] }));
      }
    }
  }

  async function handleAddComment(postId: string) {
    if (!commentBody.trim() || !token) return;
    setCommentLoading(true);
    try {
      const newComment = await api.addComment(postId, commentBody.trim(), token);
      setComments((prev) => ({
        ...prev,
        [postId]: [...(prev[postId] ?? []), newComment],
      }));
      setCommentBody('');
    } catch {
      // ignore
    } finally {
      setCommentLoading(false);
    }
  }

  function timeAgo(dateStr: string): string {
    const diff = Date.now() - new Date(dateStr).getTime();
    const hours = Math.floor(diff / 3600000);
    if (hours < 1) return 'just now';
    if (hours < 24) return `${hours}h ago`;
    const days = Math.floor(hours / 24);
    if (days < 30) return `${days}d ago`;
    return new Date(dateStr).toLocaleDateString('en-IN');
  }

  return (
    <div className="max-w-3xl mx-auto px-4 py-8">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold">Nomad Network</h1>
          <p className="text-gray-500 text-sm mt-1">Connect with digital nomads and travelers across India</p>
        </div>
        <button
          onClick={() => setShowForm(!showForm)}
          className="bg-orange-500 text-white px-4 py-2 rounded-xl text-sm font-semibold hover:bg-orange-600 transition"
        >
          {showForm ? 'Cancel' : '+ New Post'}
        </button>
      </div>

      {/* Create post form */}
      {showForm && (
        <form onSubmit={handleCreatePost} className="border rounded-2xl p-5 mb-6 bg-white">
          <h2 className="font-semibold mb-4">Create a Post</h2>
          <div className="space-y-3">
            <input
              className="w-full border rounded-lg px-3 py-2 text-sm"
              placeholder="Post title"
              value={formTitle}
              onChange={(e) => setFormTitle(e.target.value)}
              required
            />
            <textarea
              className="w-full border rounded-lg px-3 py-2 text-sm h-24 resize-none"
              placeholder="What's on your mind?"
              value={formBody}
              onChange={(e) => setFormBody(e.target.value)}
              required
            />
            <div className="flex gap-3">
              <select
                className="border rounded-lg px-3 py-2 text-sm"
                value={formCategory}
                onChange={(e) => setFormCategory(e.target.value as NomadPostCategory)}
              >
                {CATEGORIES.map((cat) => (
                  <option key={cat} value={cat}>
                    {CATEGORY_STYLE[cat].icon} {cat.replace('_', ' ')}
                  </option>
                ))}
              </select>
              <input
                className="flex-1 border rounded-lg px-3 py-2 text-sm"
                placeholder="City"
                value={formCity}
                onChange={(e) => setFormCity(e.target.value)}
                required
              />
            </div>
            {formError && <p className="text-sm text-red-500">{formError}</p>}
            <button
              type="submit"
              disabled={formLoading}
              className="bg-orange-500 text-white px-6 py-2 rounded-lg text-sm font-semibold hover:bg-orange-600 disabled:opacity-50 transition"
            >
              {formLoading ? 'Posting...' : 'Post'}
            </button>
          </div>
        </form>
      )}

      {/* Filters */}
      <div className="flex flex-col sm:flex-row gap-3 mb-6">
        <input
          className="flex-1 border rounded-lg px-3 py-2 text-sm"
          placeholder="City (e.g. Mumbai)"
          defaultValue={city}
          onBlur={(e) => updateParam('city', e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter') updateParam('city', (e.target as HTMLInputElement).value);
          }}
        />
        <div className="flex gap-2 flex-wrap">
          <button
            onClick={() => updateParam('category', '')}
            className={`px-3 py-1.5 rounded-full text-xs font-medium border transition ${
              !category ? 'bg-orange-500 text-white border-orange-500' : 'border-gray-300 hover:border-orange-400'
            }`}
          >
            All
          </button>
          {CATEGORIES.map((cat) => {
            const s = CATEGORY_STYLE[cat];
            return (
              <button
                key={cat}
                onClick={() => updateParam('category', cat)}
                className={`px-3 py-1.5 rounded-full text-xs font-medium border transition ${
                  category === cat ? 'bg-orange-500 text-white border-orange-500' : 'border-gray-300 hover:border-orange-400'
                }`}
              >
                {s.icon} {cat.replace('_', ' ')}
              </button>
            );
          })}
        </div>
      </div>

      {/* Feed */}
      {loading ? (
        <div className="space-y-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <div key={i} className="rounded-xl bg-gray-100 h-32 animate-pulse" />
          ))}
        </div>
      ) : posts.length > 0 ? (
        <div className="space-y-4">
          {posts.map((post) => {
            const catStyle = CATEGORY_STYLE[post.category] ?? CATEGORY_STYLE.TIP;
            const isExpanded = expandedPost === post.id;
            const postComments = comments[post.id] ?? [];

            return (
              <div key={post.id} className="border rounded-2xl bg-white overflow-hidden">
                <div className="p-5">
                  <div className="flex items-start gap-3">
                    <div className="w-10 h-10 rounded-full bg-orange-100 flex items-center justify-center shrink-0 text-lg">
                      {catStyle.icon}
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className="font-semibold text-sm">{post.authorName}</span>
                        <span className="text-xs text-gray-400">{timeAgo(post.createdAt)}</span>
                        <span className={`text-xs font-semibold px-2 py-0.5 rounded-full ${catStyle.bg} ${catStyle.text}`}>
                          {post.category.replace('_', ' ')}
                        </span>
                        <span className="text-xs text-gray-400">{post.city}</span>
                      </div>
                      <h3 className="font-semibold mt-2">{post.title}</h3>
                      <p className="text-sm text-gray-600 mt-1 whitespace-pre-wrap">{post.body}</p>
                    </div>
                  </div>

                  <div className="flex items-center gap-4 mt-4 text-sm text-gray-500">
                    <span>&#9650; {post.upvotes}</span>
                    <button
                      onClick={() => toggleComments(post.id)}
                      className="hover:text-orange-500 transition"
                    >
                      &#128172; {post.commentCount} comment{post.commentCount !== 1 ? 's' : ''}
                    </button>
                  </div>
                </div>

                {/* Comments section */}
                {isExpanded && (
                  <div className="border-t bg-gray-50 p-5">
                    {postComments.length > 0 ? (
                      <div className="space-y-3 mb-4">
                        {postComments.map((cmt) => (
                          <div key={cmt.id} className="flex gap-2">
                            <div className="w-7 h-7 rounded-full bg-gray-200 flex items-center justify-center shrink-0 text-xs font-bold text-gray-500">
                              {cmt.authorName.charAt(0).toUpperCase()}
                            </div>
                            <div className="flex-1">
                              <div className="flex items-center gap-2">
                                <span className="text-xs font-semibold">{cmt.authorName}</span>
                                <span className="text-xs text-gray-400">{timeAgo(cmt.createdAt)}</span>
                              </div>
                              <p className="text-sm text-gray-600 mt-0.5">{cmt.body}</p>
                            </div>
                          </div>
                        ))}
                      </div>
                    ) : (
                      <p className="text-sm text-gray-400 mb-4">No comments yet. Be the first!</p>
                    )}

                    {token ? (
                      <div className="flex gap-2">
                        <input
                          className="flex-1 border rounded-lg px-3 py-2 text-sm bg-white"
                          placeholder="Write a comment..."
                          value={expandedPost === post.id ? commentBody : ''}
                          onChange={(e) => setCommentBody(e.target.value)}
                          onKeyDown={(e) => {
                            if (e.key === 'Enter') handleAddComment(post.id);
                          }}
                        />
                        <button
                          onClick={() => handleAddComment(post.id)}
                          disabled={commentLoading || !commentBody.trim()}
                          className="bg-orange-500 text-white px-4 py-2 rounded-lg text-sm font-semibold hover:bg-orange-600 disabled:opacity-50 transition"
                        >
                          {commentLoading ? '...' : 'Send'}
                        </button>
                      </div>
                    ) : (
                      <a href="/auth?redirect=/nomad" className="text-sm text-orange-500 font-semibold hover:underline">
                        Sign in to comment &rarr;
                      </a>
                    )}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      ) : (
        <div className="text-center py-20 text-gray-400">
          <p className="text-5xl mb-4">🌍</p>
          <p className="text-lg font-medium">No posts yet in {city}</p>
          <p className="text-sm mt-1">Be the first to share something with the community!</p>
        </div>
      )}
    </div>
  );
}
