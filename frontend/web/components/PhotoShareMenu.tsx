'use client';

interface Props {
  imageUrl: string;
  listingName: string;
  onClose: () => void;
}

export default function PhotoShareMenu({ imageUrl, listingName, onClose }: Props) {
  const shareToWhatsApp = () => {
    window.open(`https://wa.me/?text=${encodeURIComponent(`Check out ${listingName} on Safar: ${imageUrl}`)}`, '_blank');
  };

  const copyLink = async () => {
    await navigator.clipboard.writeText(imageUrl);
    onClose();
  };

  const download = () => {
    const a = document.createElement('a');
    a.href = imageUrl;
    a.download = `safar-${listingName.replace(/\s+/g, '-')}.jpg`;
    a.click();
  };

  return (
    <div className="absolute bottom-16 right-4 bg-white rounded-xl shadow-xl p-2 z-60 min-w-[180px]">
      <button onClick={copyLink} className="w-full text-left px-4 py-2 text-sm hover:bg-gray-100 rounded-lg">
        Copy link
      </button>
      <button onClick={shareToWhatsApp} className="w-full text-left px-4 py-2 text-sm hover:bg-gray-100 rounded-lg">
        Share to WhatsApp
      </button>
      <button onClick={download} className="w-full text-left px-4 py-2 text-sm hover:bg-gray-100 rounded-lg">
        Download photo
      </button>
    </div>
  );
}
